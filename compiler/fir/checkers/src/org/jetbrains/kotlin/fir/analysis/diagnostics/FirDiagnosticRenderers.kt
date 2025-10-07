/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.UnsafeExpressionUtility
import org.jetbrains.kotlin.fir.expressions.toReferenceUnsafe
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ReturnValueStatus
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.text.MessageFormat

@Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")
object FirDiagnosticRenderers {
    val SYMBOL: ContextIndependentParameterRenderer<FirBasedSymbol<*>> = SymbolRenderer(modifierRenderer = ::FirPartialModifierRenderer)

    val SYMBOL_WITH_ALL_MODIFIERS: ContextIndependentParameterRenderer<FirBasedSymbol<*>> = SymbolRenderer()

    private open class SymbolRenderer<T : FirBasedSymbol<*>>(
        val useCallableSignatureRenderer: Boolean = true,
        val useSupertypeRenderer: Boolean = true,
        val startErrorTypeIndex: Int = 0,
        val modifierRenderer: () -> FirModifierRenderer? = ::FirAllModifierRenderer,
    ) : ContextIndependentParameterRenderer<T> {
        override fun render(obj: T): String {
            return renderWithTail(obj).parameter
        }

        @OptIn(SymbolInternals::class)
        override fun renderWithTail(obj: T): ParameterWithTail {
            var tail: List<String>? = null
            val result = when (obj) {
                is FirClassLikeSymbol<*>, is FirCallableSymbol<*> -> {
                    val renderer = FirRenderer(
                        typeRenderer = ConeTypeRendererForReadability(startErrorTypeIndex = startErrorTypeIndex) { ConeIdShortRenderer() },
                        idRenderer = ConeIdShortRenderer(),
                        classMemberRenderer = FirNoClassMemberRenderer(),
                        bodyRenderer = null,
                        propertyAccessorRenderer = null,
                        callArgumentsRenderer = FirCallNoArgumentsRenderer(),
                        modifierRenderer = modifierRenderer(),
                        callableSignatureRenderer = runIf(useCallableSignatureRenderer) { FirCallableSignatureRendererForReadability() },
                        declarationRenderer = FirDeclarationRenderer("local "),
                        annotationRenderer = null,
                        contractRenderer = null,
                        supertypeRenderer = runIf(useSupertypeRenderer) { FirSupertypeRenderer() },
                        lineBreakAfterContextParameters = false,
                        renderFieldAnnotationSeparately = false,
                    )
                    renderer.renderElementAsString(obj.fir, trim = true).also {
                        tail = (renderer.typeRenderer as ConeTypeRendererForReadability).typeConstructorReadableDescriptions.takeIf {
                            it.isNotEmpty()
                        }
                    }
                }
                is FirTypeParameterSymbol -> obj.name.asString()
                else -> "???"
            }
            return ParameterWithTail(result, tail)
        }

    }

    val CALLABLE_FQ_NAME: ContextIndependentParameterRenderer<FirCallableSymbol<*>> = SymbolRendererCallableFqName()

    private class SymbolRendererCallableFqName : SymbolRenderer<FirCallableSymbol<*>>(
        modifierRenderer = ::FirPartialModifierRenderer
    ) {
        override fun renderWithTail(obj: FirCallableSymbol<*>): ParameterWithTail {
            val result = super.renderWithTail(obj)
            val origin = obj.containingClassLookupTag()?.classId?.asFqNameString()
            return ParameterWithTail(result.parameter + origin?.let { ", defined in $it" }.orEmpty(), result.tail)
        }
    }

    val SYMBOL_WITH_CONTAINING_DECLARATION: ContextIndependentParameterRenderer<FirBasedSymbol<*>> =
        SymbolRendererWithContainingDeclaration()

    private class SymbolRendererWithContainingDeclaration : SymbolRenderer<FirBasedSymbol<*>>(
        modifierRenderer = ::FirPartialModifierRenderer
    ) {
        override fun renderWithTail(obj: FirBasedSymbol<*>): ParameterWithTail {
            val result = super.renderWithTail(obj)
            val containingClassId = when (obj) {
                is FirCallableSymbol<*> -> obj.callableId?.classId
                is FirTypeParameterSymbol -> (obj.containingDeclarationSymbol as? FirClassLikeSymbol<*>)?.classId
                else -> null
            } ?: return ParameterWithTail("'${result.parameter}'", result.tail)
            return ParameterWithTail(
                "'${result.parameter}' defined in ${NAME_OF_DECLARATION_OR_FILE.render(containingClassId)}",
                result.tail
            )
        }
    }

    val TYPE_PARAMETER_OWNER_SYMBOL: ContextIndependentParameterRenderer<FirBasedSymbol<*>> =
        SymbolRenderer(useCallableSignatureRenderer = false, useSupertypeRenderer = false) { null }

    /**
     * Adds a line break before the list, then prints one symbol per line.
     */
    val SYMBOLS_ON_NEXT_LINES = SymbolCollectionRenderer()

    class SymbolCollectionRenderer(
        val prefix: String = "\n",
        val pluralPrefix: String = prefix,
    ) : ContextIndependentParameterRenderer<Collection<FirBasedSymbol<*>>> {
        override fun render(obj: Collection<FirBasedSymbol<*>>): String {
            return renderWithTail(obj).parameter
        }

        override fun renderWithTail(obj: Collection<FirBasedSymbol<*>>): ParameterWithTail {
            val collectionTail = mutableListOf<String>()
            val collectionResult = obj.joinToString(
                separator = "\n",
                prefix = if (obj.isEmpty()) "" else if (obj.size == 1) prefix else pluralPrefix,
            ) { symbol ->
                val symbolRenderer = SymbolRenderer<FirBasedSymbol<*>>(
                    startErrorTypeIndex = collectionTail.size,
                    modifierRenderer = ::FirPartialModifierRenderer
                )

                symbolRenderer.renderWithTail(symbol).also {
                    collectionTail += it.tail.orEmpty()
                }.parameter
            }
            return ParameterWithTail(collectionResult, collectionTail)
        }

    }

    val MEMBER_SYMBOL_COLLECTION_RENDERER = SymbolCollectionRenderer(prefix = "member:\n", pluralPrefix = "members:\n")

    private class SymbolWithDiagnosticMessagesCollectionRenderer :
        ContextIndependentParameterRenderer<Collection<Pair<FirBasedSymbol<*>, List<String>>>> {
        override fun render(obj: Collection<Pair<FirBasedSymbol<*>, List<String>>>): String {
            return renderWithTail(obj).parameter
        }

        override fun renderWithTail(obj: Collection<Pair<FirBasedSymbol<*>, List<String>>>): ParameterWithTail {
            val collectionTail = mutableListOf<String>()
            val collectionResult = obj.joinToString(
                separator = "\n",
            ) { (symbol, diagnostics) ->
                val symbolRenderer = SymbolRenderer<FirBasedSymbol<*>>(
                    startErrorTypeIndex = collectionTail.size,
                    modifierRenderer = ::FirPartialModifierRenderer
                )

                buildString {
                    append(
                        symbolRenderer.renderWithTail(symbol).also {
                            collectionTail += it.tail.orEmpty()
                        }.parameter
                    )

                    if (diagnostics.isNotEmpty()) {
                        appendLine(":")

                        diagnostics.forEach {
                            append("  ")
                            appendLine(it)
                        }
                    }
                }
            }
            return ParameterWithTail(collectionResult, collectionTail)
        }
    }

    val CANDIDATES_WITH_DIAGNOSTIC_MESSAGES: ContextIndependentParameterRenderer<Collection<Pair<FirBasedSymbol<*>, List<String>>>> =
        SymbolWithDiagnosticMessagesCollectionRenderer()

    fun <Q> formatted(message: String, renderer: DiagnosticParameterRenderer<Q>): DiagnosticParameterRenderer<Q> =
        ContextDependentRenderer { value, context ->
            MessageFormat(message).format(arrayOf(renderer.render(value, context)))
        }

    fun <Q : Any> emptyStringIfNullOr(renderer: DiagnosticParameterRenderer<Q>): DiagnosticParameterRenderer<Q?> =
        ContextDependentRenderer { value, context ->
            value?.let { renderer.render(it, context) } ?: ""
        }

    /**
     * Formats the formatted [message] if the value is not `null`.
     * Returns an empty string otherwise.
     */
    fun <Q : Any> suggestIfNotNull(message: String, renderer: DiagnosticParameterRenderer<Q>): DiagnosticParameterRenderer<Q?> =
        emptyStringIfNullOr(formatted(message, renderer))

    val SYMBOLS_ON_NEWLINE_WITH_INDENT = object : ContextIndependentParameterRenderer<Collection<FirCallableSymbol<*>>> {
        private val mode = MultiplatformDiagnosticRenderingMode()

        override fun render(obj: Collection<FirCallableSymbol<*>>): String {
            return buildString {
                for (symbol in obj) {
                    mode.newLine(this)
                    mode.renderSymbol(this, symbol, "")
                }
            }
        }
    }

    val CALLABLES_FQ_NAMES = object : ContextIndependentParameterRenderer<Collection<FirCallableSymbol<*>>> {
        override fun render(obj: Collection<FirCallableSymbol<*>>) = "\n" + obj.joinToString("\n") { symbol ->
            INDENTATION_UNIT + CALLABLE_FQ_NAME.render(symbol)
        } + "\n"
    }

    val RENDER_COLLECTION_OF_TYPES = ContextDependentRenderer { types: Collection<ConeKotlinType>, ctx ->
        types.joinToString(separator = ", ") { type ->
            RENDER_TYPE.render(type, ctx)
        }
    }

    val CALLEE_NAME = Renderer { element: FirExpression ->
        @OptIn(UnsafeExpressionUtility::class)
        when (val reference = element.unwrapSmartcastExpression().toReferenceUnsafe()) {
            is FirNamedReference -> reference.name.asString()
            is FirThisReference -> "this"
            is FirSuperReference -> "super"
            else -> "???"
        }
    }

    val VARIABLE_NAME = Renderer { symbol: FirVariableSymbol<*> ->
        symbol.name.asString()
    }

    val DECLARATION_NAME = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirCallableSymbol<*> -> symbol.name.asString()
            is FirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
            is FirTypeParameterSymbol -> symbol.name.asString()
            else -> return@Renderer "???"
        }
    }

    val DECLARATION_FQ_NAME = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirCallableSymbol<*> -> symbol.name.asString()
            is FirClassLikeSymbol<*> -> symbol.classId.asFqNameString()
            else -> return@Renderer "???"
        }
    }

    val RENDER_CLASS_OR_OBJECT_QUOTED = Renderer { classSymbol: FirClassSymbol<*> ->
        val name = classSymbol.classId.relativeClassName.asString()
        val classOrObject = when (classSymbol.classKind) {
            ClassKind.OBJECT -> "Object"
            ClassKind.INTERFACE -> "Interface"
            else -> "Class"
        }
        "$classOrObject '$name'"
    }

    val RENDER_ENUM_ENTRY_QUOTED = Renderer { enumEntry: FirEnumEntrySymbol ->
        var name = enumEntry.callableId.callableName.asString()
        enumEntry.callableId.classId?.let {
            name = "${it.shortClassName.asString()}.$name"
        }
        "Enum entry '$name'"
    }

    fun RENDER_CLASS_OR_OBJECT(quoted: Boolean, name: (ClassId) -> String) = Renderer { firClassLike: FirClassLikeSymbol<*> ->
        val name = name(firClassLike.classId)
        val prefix = when (firClassLike) {
            is FirTypeAliasSymbol -> "typealias"
            is FirRegularClassSymbol -> {
                when {
                    firClassLike.isCompanion -> "companion object"
                    firClassLike.isInterface -> "interface"
                    firClassLike.isEnumClass -> "enum class"
                    firClassLike.isFromEnumClass -> "enum entry"
                    firClassLike.isLocal -> "object"
                    else -> "class"
                }
            }
            else -> AssertionError("Unexpected class: $firClassLike")
        }
        if (quoted) "$prefix '$name'" else "$prefix $name"
    }

    val RENDER_CLASS_OR_OBJECT_NAME_QUOTED =
        RENDER_CLASS_OR_OBJECT(quoted = true) { classId -> classId.relativeClassName.shortName().asString() }

    val STAR_PROJECTED_CLASS = Renderer { symbol: FirClassLikeSymbol<*> ->
        val list = buildList {
            var current: FirClassLikeSymbol<*>? = symbol
            var requiresParameters = true
            while (current != null) {
                add(buildString {
                    append(current.classId.shortClassName)

                    val parameterCount = current.ownTypeParameterSymbols.size
                    if (requiresParameters && parameterCount > 0) {
                        append("<")
                        Array(parameterCount) { "*" }.joinTo(this, ", ")
                        append(">")
                    }
                })

                requiresParameters = current.isInner
                current = current.getContainingClassSymbol()
            }
        }
        list.reversed().joinToString(".")
    }

    val RENDER_TYPE: DiagnosticParameterRenderer<ConeKotlinType> = AdaptiveTypeRenderer()

    private class AdaptiveTypeRenderer() : DiagnosticParameterRenderer<ConeKotlinType> {
        override fun render(obj: ConeKotlinType, renderingContext: RenderingContext): String {
            return renderWithTail(obj, renderingContext).parameter
        }

        override fun renderWithTail(obj: ConeKotlinType, renderingContext: RenderingContext): ParameterWithTail {
            // TODO, KT-59811: need a way to tune granularity, e.g., without parameter names in functional types.
            val representation = renderingContext[ADAPTIVE_RENDERED_TYPES]
            val tail = representation.descriptions.takeIf { it.isNotEmpty() }
            return ParameterWithTail(representation[obj], tail)
        }

    }

    val RENDER_FQ_NAME_WITH_PREFIX = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirCallableSymbol<*> -> CALLABLE_FQ_NAME.render(symbol)
            is FirClassLikeSymbol<*> -> RENDER_CLASS_OR_OBJECT(quoted = false) { classId -> classId.asFqNameString() }.render(symbol)
            else -> return@Renderer "???"
        }
    }

    private class AdaptiveTypesRepresentation(val strings: Map<ConeKotlinType, String>, val descriptions: List<String>) {
        operator fun get(type: ConeKotlinType) = strings.getValue(type)
    }

    private val ADAPTIVE_RENDERED_TYPES: RenderingContext.Key<AdaptiveTypesRepresentation> =
        object : RenderingContext.Key<AdaptiveTypesRepresentation>("ADAPTIVE_RENDERED_TYPES") {
            override fun compute(objectsToRender: Collection<Any?>): AdaptiveTypesRepresentation {
                val coneTypes = objectsToRender.filterIsInstance<ConeKotlinType>() +
                        objectsToRender.filterIsInstance<Iterable<*>>().flatMap { it.filterIsInstance<ConeKotlinType>() }

                val constructors = buildSet {
                    coneTypes.forEach {
                        it.forEachType { typeWithinIt ->
                            val lowerBound = typeWithinIt.lowerBoundIfFlexible()

                            if (lowerBound !is ConeIntersectionType) {
                                add(lowerBound.getConstructor())
                            }
                        }
                    }
                }

                val errorTypeDescriptions = mutableListOf<String>()
                val simpleRepresentationsByConstructor: Map<TypeConstructorMarker, String> = constructors.associateWith {
                    buildString {
                        val renderer = ConeTypeRendererForReadability(
                            this, startErrorTypeIndex = errorTypeDescriptions.size
                        ) { ConeIdShortRenderer() }
                        renderer.renderConstructor(it.delegatedConstructorOrSelf())
                        errorTypeDescriptions += renderer.typeConstructorReadableDescriptions
                    }
                }

                val constructorsByRepresentation: Map<String, List<TypeConstructorMarker>> =
                    simpleRepresentationsByConstructor.entries.groupBy({ it.value }, { it.key })

                val finalRepresentationsByConstructor: Map<TypeConstructorMarker, String> = constructors.associateWith {
                    val representation = simpleRepresentationsByConstructor.getValue(it)

                    val typesWithSameRepresentation = constructorsByRepresentation.getValue(representation)
                    val isAmbiguous = typesWithSameRepresentation.size > 1
                    val isError = it is ConeClassLikeErrorLookupTag
                    val isTypeParameter = it !is ConeTypeParameterLookupTag && !isError
                    val isClassLike = it is ConeClassLikeLookupTag && !isError

                    if (!isAmbiguous && isTypeParameter) {
                        return@associateWith "$representation^"
                    }

                    buildString {
                        if (isError && it.diagnostic is ConeCannotInferTypeParameterType) {
                            append("uninferred ")
                        }

                        if (isClassLike && isAmbiguous) {
                            ConeTypeRendererForReadability(this) { ConeIdRendererForDiagnostics() }.renderConstructor(it)
                        } else {
                            append(representation)
                        }

                        if (!isClassLike && !isError && isAmbiguous) {
                            append('#')
                            append(typesWithSameRepresentation.indexOf(it) + 1)
                        }
                        // Special symbol to be replaced with a nullability marker, like "", "?", "!", or maybe something else in future
                        append("^")

                        val typeParameterSymbol =
                            ((it as? ConeClassLikeErrorLookupTag)?.delegatedType?.lowerBoundIfFlexible()
                                ?.getConstructor() as? ConeTypeParameterLookupTag)?.typeParameterSymbol
                                ?: (it as? ConeTypeParameterLookupTag)?.typeParameterSymbol

                        if (typeParameterSymbol != null) {
                            append(" (of ")
                            append(TYPE_PARAMETER_OWNER_SYMBOL.render(typeParameterSymbol.containingDeclarationSymbol))
                            append(')')
                        }
                    }
                }

                return AdaptiveTypesRepresentation(
                    coneTypes.associateWith {
                        it.renderReadableWithFqNames(finalRepresentationsByConstructor)
                    },
                    errorTypeDescriptions
                )
            }

            private fun TypeConstructorMarker.delegatedConstructorOrSelf(): TypeConstructorMarker {
                return if (this is ConeClassLikeErrorLookupTag && this.diagnostic is ConeCannotInferTypeParameterType) {
                    this.delegatedType?.lowerBoundIfFlexible()?.getConstructor() ?: this
                } else {
                    this
                }
            }
        }

    // TODO: properly implement
    val RENDER_TYPE_WITH_ANNOTATIONS = RENDER_TYPE

    private const val WHEN_MISSING_LIMIT = 7

    val WHEN_MISSING_CASES = Renderer { missingCases: List<WhenMissingCase> ->
        if (missingCases.singleOrNull() == WhenMissingCase.Unknown) {
            "an 'else' branch"
        } else {
            val list = missingCases.joinToString(", ", limit = WHEN_MISSING_LIMIT) { "'$it'" }
            val branches = if (missingCases.size > 1) "branches" else "branch"
            "the $list $branches or an 'else' branch"
        }
    }

    val MODULE_DATA = Renderer<FirModuleData> {
        "module ${it.name}"
    }

    val NAME_OF_CONTAINING_DECLARATION_OR_FILE = Renderer { symbol: CallableId ->
        NAME_OF_DECLARATION_OR_FILE.render(symbol.classId)
    }

    val NAME_OF_DECLARATION_OR_FILE = Renderer { classId: ClassId? ->
        if (classId == null) {
            "file"
        } else {
            "'${classId.asFqNameString()}'"
        }
    }

    val FUNCTIONAL_TYPE_KIND = Renderer { kind: FunctionTypeKind ->
        kind.prefixForTypeRender ?: kind.classNamePrefix
    }

    val FUNCTIONAL_TYPE_KINDS = KtDiagnosticRenderers.COLLECTION(FUNCTIONAL_TYPE_KIND)

    val REQUIRE_KOTLIN_VERSION = Renderer { version: VersionRequirement.Version ->
        if (version == VersionRequirement.Version.INFINITY) "" else " is only available since Kotlin ${version.asString()} and"
    }

    val OPTIONAL_SENTENCE = Renderer { it: String? ->
        if (!it.isNullOrBlank()) {
            buildString {
                append(" ")
                append(it.trim())
                if (!endsWith(".")) {
                    append(".")
                }
            }
        } else {
            ""
        }
    }

    val FOR_OPTIONAL_OPERATOR = Renderer { it: String? ->
        if (!it.isNullOrBlank()) " for operator '$it'" else ""
    }

    val OF_OPTIONAL_NAME = Renderer { name: Name? ->
        name?.asString()?.takeIf { it.isNotBlank() }?.let { " of '$it'" } ?: ""
    }

    val IGNORABILITY_STATUS = Renderer { status: ReturnValueStatus ->
        when (status) {
            ReturnValueStatus.MustUse -> "must-use"
            ReturnValueStatus.ExplicitlyIgnorable -> "ignorable"
            ReturnValueStatus.Unspecified -> "unspecified (implicitly ignorable)"
        }
    }

    val SYMBOL_KIND = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirPropertyAccessorSymbol -> "property accessor"
            is FirConstructorSymbol -> "constructor"
            is FirFunctionSymbol -> "function"
            is FirPropertySymbol -> "property"
            is FirBackingFieldSymbol -> "backing field"
            is FirDelegateFieldSymbol -> "delegate field"
            is FirEnumEntrySymbol -> "enum entry"
            is FirFieldSymbol -> "field"
            is FirValueParameterSymbol -> "value parameter"
            is FirFileSymbol -> "file"
            is FirAnonymousInitializerSymbol -> "initializer"
            is FirTypeParameterSymbol -> "type parameter"
            is FirRegularClassSymbol -> when (symbol.classKind) {
                ClassKind.CLASS -> "class"
                ClassKind.INTERFACE -> "interface"
                ClassKind.ENUM_CLASS -> "enum class"
                ClassKind.ENUM_ENTRY -> "enum entry"
                ClassKind.ANNOTATION_CLASS -> "annotation class"
                ClassKind.OBJECT -> "object"
            }
            is FirAnonymousObjectSymbol -> "anonymous object"
            is FirTypeAliasSymbol -> "type alias"
            else -> "declaration"
        }
    }

    val KOTLIN_TARGETS = Renderer { targets: Collection<KotlinTarget> ->
        targets.joinToString { it.description }
    }

    val STRING_TARGETS = Renderer { targets: Collection<String> ->
        val quotedTargets = targets.joinToString { "'$it'" }
        when (targets.size) {
            0 -> "no targets"
            1 -> "target $quotedTargets"
            else -> "targets $quotedTargets"
        }
    }
}

fun <T> DiagnosticParameterRenderer<T>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
): DiagnosticParameterRenderer<Iterable<T>> = ContextDependentRenderer { types: Iterable<T>, ctx ->
    types.joinToString(separator, prefix, postfix, limit, truncated) { render(it, ctx) }
}
