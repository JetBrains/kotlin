/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.ContextDependentRenderer
import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.getConstructor
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.renderReadableWithFqNames
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

@Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")
object FirDiagnosticRenderers {
    @OptIn(SymbolInternals::class)
    val SYMBOL = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirClassLikeSymbol, is FirCallableSymbol -> FirRenderer(
                typeRenderer = ConeTypeRendererForReadability { ConeIdShortRenderer() },
                idRenderer = ConeIdShortRenderer(),
                classMemberRenderer = FirNoClassMemberRenderer(),
                bodyRenderer = null,
                propertyAccessorRenderer = null,
                callArgumentsRenderer = FirCallNoArgumentsRenderer(),
                modifierRenderer = FirPartialModifierRenderer(),
                valueParameterRenderer = FirValueParameterRendererForReadability(),
                declarationRenderer = FirDeclarationRenderer("local "),
                annotationRenderer = null,
                lineBreakAfterContextReceivers = false,
                renderFieldAnnotationSeparately = false,
            ).renderElementAsString(symbol.fir, trim = true)
            is FirTypeParameterSymbol -> symbol.name.asString()
            else -> "???"
        }
    }

    /**
     * Adds a line break before the list, then prints one symbol per line.
     */
    val SYMBOLS_ON_NEXT_LINES = Renderer { symbols: Collection<FirBasedSymbol<*>> ->
        symbols.joinToString(separator = "\n", prefix = "\n", transform = SYMBOL::render)
    }

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
            val origin = symbol.containingClassLookupTag()?.classId?.asFqNameString()
            INDENTATION_UNIT + SYMBOL.render(symbol) + origin?.let { ", defined in $it" }.orEmpty()
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
        val name = when (symbol) {
            is FirCallableSymbol<*> -> symbol.name
            is FirClassLikeSymbol<*> -> symbol.classId.shortClassName
            else -> return@Renderer "???"
        }
        name.asString()
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

    val RENDER_CLASS_OR_OBJECT_NAME_QUOTED = Renderer { firClassLike: FirClassLikeSymbol<*> ->
        val name = firClassLike.classId.relativeClassName.shortName().asString()
        val prefix = when (firClassLike) {
            is FirTypeAliasSymbol -> "typealias"
            is FirRegularClassSymbol -> {
                when {
                    firClassLike.isCompanion -> "companion object"
                    firClassLike.isInterface -> "interface"
                    firClassLike.isEnumClass -> "enum class"
                    firClassLike.isFromEnumClass -> "enum entry"
                    firClassLike.isLocalClassOrAnonymousObject -> "object"
                    else -> "class"
                }
            }
            else -> AssertionError("Unexpected class: $firClassLike")
        }
        "$prefix '$name'"
    }

    val RENDER_TYPE = ContextDependentRenderer { t: ConeKotlinType, ctx ->
        // TODO, KT-59811: need a way to tune granuality, e.g., without parameter names in functional types.
        ctx[ADAPTIVE_RENDERED_TYPES].getValue(t)
    }

    private val ADAPTIVE_RENDERED_TYPES: RenderingContext.Key<Map<ConeKotlinType, String>> =
        object : RenderingContext.Key<Map<ConeKotlinType, String>>("ADAPTIVE_RENDERED_TYPES") {
            override fun compute(objectsToRender: Collection<Any?>): Map<ConeKotlinType, String> {
                val coneTypes = objectsToRender.filterIsInstance<ConeKotlinType>() +
                        objectsToRender.filterIsInstance<Iterable<*>>().flatMap { it.filterIsInstance<ConeKotlinType>() }

                val constructors = buildSet {
                    coneTypes.forEach {
                        it.forEachType {
                            if (it !is ConeIntersectionType) {
                                add(it.lowerBoundIfFlexible().getConstructor())
                            }
                        }
                    }
                }

                val simpleRepresentationsByConstructor: Map<TypeConstructorMarker, String> = constructors.associateWith {
                    buildString { ConeTypeRendererForReadability(this) { ConeIdRendererForDiagnostics() }.renderConstructor(it) }
                }

                val constructorsByRepresentation: Map<String, List<TypeConstructorMarker>> =
                    simpleRepresentationsByConstructor.entries.groupBy({ it.value }, { it.key })

                val finalRepresentationsByConstructor: Map<TypeConstructorMarker, String> = constructors.associateWith {
                    val representation = simpleRepresentationsByConstructor.getValue(it)

                    val typesWithSameRepresentation = constructorsByRepresentation.getValue(representation)
                    if (typesWithSameRepresentation.size == 1) return@associateWith representation

                    val index = typesWithSameRepresentation.indexOf(it) + 1

                    buildString {
                        append(representation)
                        append('#')
                        append(index)

                        if (it is ConeTypeParameterLookupTag) {
                            append(" (type parameter of ")
                            append(SYMBOL.render(it.typeParameterSymbol.containingDeclarationSymbol))
                            append(')')
                        }
                    }
                }

                return coneTypes.associateWith {
                    it.renderReadableWithFqNames(finalRepresentationsByConstructor)
                }
            }
        }

    // TODO: properly implement
    val RENDER_TYPE_WITH_ANNOTATIONS = RENDER_TYPE

    val AMBIGUOUS_CALLS = Renderer { candidates: Collection<FirBasedSymbol<*>> ->
        candidates.joinToString(separator = "\n", prefix = "\n") { symbol ->
            SYMBOL.render(symbol)
        }
    }

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
            "'${classId}'"
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

    val SYMBOL_WITH_CONTAINING_DECLARATION = Renderer { symbol: FirCallableSymbol<*> ->
        "'${SYMBOL.render(symbol)}' defined in ${NAME_OF_CONTAINING_DECLARATION_OR_FILE.render(symbol.callableId)}"
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
}
