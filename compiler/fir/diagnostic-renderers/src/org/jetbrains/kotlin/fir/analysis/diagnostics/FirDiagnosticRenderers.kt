/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticBaseContext
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.UnsafeExpressionUtility
import org.jetbrains.kotlin.fir.expressions.toReferenceUnsafe
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.resolve.ReturnValueStatus
import org.jetbrains.kotlin.toLinesMapping
import org.jetbrains.kotlin.utils.addToStdlib.partitionNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.text.MessageFormat

@Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")
object FirDiagnosticRenderers {
    object DiagnosticStaticPolicy : FirModifierRenderer.StaticPolicy {
        override fun renderStatic(memberDeclaration: FirDeclaration): String? {
            if (memberDeclaration is FirEnumEntry) return null
            return if (memberDeclaration.isJavaOrEnhancement) "static" else "companion"
        }
    }

    val SYMBOL = symbolRenderer(
        modifierRenderer = { FirPartialModifierRenderer(DiagnosticStaticPolicy) }
    )

    val SYMBOL_WITH_ALL_MODIFIERS = symbolRenderer()

    @OptIn(SymbolInternals::class)
    private fun symbolRenderer(
        modifierRenderer: () -> FirModifierRenderer? = { FirAllModifierRenderer(DiagnosticStaticPolicy) },
    ) = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirClassLikeSymbol, is FirCallableSymbol -> FirRenderer(
                typeRenderer = ConeTypeRendererForReadability { ConeIdShortRenderer() },
                idRenderer = ConeIdShortRenderer(),
                classMemberRenderer = FirNoClassMemberRenderer(),
                bodyRenderer = null,
                propertyAccessorRenderer = null,
                callArgumentsRenderer = FirCallNoArgumentsRenderer(),
                modifierRenderer = modifierRenderer(),
                callableSignatureRenderer = FirCallableSignatureRendererForReadability(),
                declarationRenderer = FirDeclarationRenderer("local "),
                contractRenderer = null,
                annotationRenderer = null,
                lineBreakAfterContextParameters = false,
                renderFieldAnnotationSeparately = false,
            ).renderElementAsString(symbol.fir, trim = true)
            is FirTypeParameterSymbol -> symbol.name.asString()
            else -> "???"
        }
    }

    val SYMBOL_WITH_LOCATION = Renderer { symbol: DeclarationSymbolMarker ->
        when (symbol) {
            is FirClassLikeSymbol<*>, is FirCallableSymbol<*> -> symbol.renderWithLocation(SYMBOL::render)
            is FirTypeParameterSymbol -> symbol.renderWithLocation { symbol.name.asString() }
            else -> ""
        }
    }

    private inline fun FirBasedSymbol<*>.renderWithLocation(render: (FirBasedSymbol<*>) -> String): String {
        val logicalLocation = toLogicalLocation()
        val node = listOf(this).groupIntoSymbolNodes()
        return when (val physicalLocation = toPhysicalLocation()) {
            null -> decorateIncompleteLocation(logicalLocation, node, render)
            else -> decorateCompleteLocation(logicalLocation, FileNode(physicalLocation.filePath, node), render)
        }
    }

    private data class LogicalLocationNode(
        val location: LogicalLocation,
        val completeSymbols: List<FileNode>,
        val incompleteSymbols: List<SymbolNode>,
    )

    private data class FileNode(
        val filePath: String,
        val symbols: List<SymbolNode>,
    )

    private data class SymbolNode(
        val line: Int?,
        val column: Int?,
        val symbol: FirBasedSymbol<*>,
        var children: MutableCollection<SymbolNode>,
    )

    private fun FirBasedSymbol<*>.toSymbolNode(children: MutableCollection<SymbolNode> = mutableListOf()): SymbolNode {
        val physicalLocation = toPhysicalLocation()
        return SymbolNode(physicalLocation?.line, physicalLocation?.column, this, children)
    }

    private fun Iterable<FirBasedSymbol<*>>.groupIntoSymbolNodes(): List<SymbolNode> {
        val containerToNode = mutableMapOf<FirBasedSymbol<*>, SymbolNode>()
        val symbolChains = map { symbol ->
            val chain = symbol.collectContainers() + symbol
            chain.map { containerToNode.getOrPut(it) { it.toSymbolNode(children = mutableSetOf()) } }
        }
        for (chain in symbolChains) {
            for (index in 1 until chain.size) {
                chain[index - 1].children.add(chain[index])
            }
        }
        for (chain in symbolChains) {
            for (node in chain) {
                node.children = node.children.sortedByDescending { it.line ?: 0 }.toMutableList()
            }
        }
        return symbolChains.mapTo(mutableSetOf()) { it.first() }.sortedBy { it.line }
    }

    private fun Iterable<Pair<String, FirBasedSymbol<*>>>.groupIntoFileNodes(): List<FileNode> =
        groupBy({ it.first }, { it.second }).entries.map {
            FileNode(it.key, it.value.groupIntoSymbolNodes())
        }

    private fun Iterable<FirBasedSymbol<*>>.groupIntoLogicalLocations(): List<LogicalLocationNode> =
        groupBy { it.toLogicalLocation() }.map { [location, symbols] ->
            val [completeSymbols, incompleteSymbols] = symbols.partitionNotNull { it.toPhysicalLocation()?.filePath?.to(it) }
            LogicalLocationNode(location, completeSymbols.groupIntoFileNodes(), incompleteSymbols.groupIntoSymbolNodes())
        }

    private inline fun Iterable<FirBasedSymbol<*>>.renderWithLocationInGroups(render: (FirBasedSymbol<*>) -> String): String =
        groupIntoLogicalLocations()
            .flatMap { location ->
                val blocksForComplete = location.completeSymbols.map {
                    decorateCompleteLocation(location.location, it, render)
                }
                val blockForIncomplete = runIf(location.incompleteSymbols.isNotEmpty()) {
                    decorateIncompleteLocation(location.location, location.incompleteSymbols, render)
                }
                blocksForComplete + listOfNotNull(blockForIncomplete)
            }
            .joinToString("\n")

    private data class RenderedLineWithNumber(
        val number: Int?,
        val renderedLine: String,
    )

    private inline fun FirBasedSymbol<*>.renderToLinesWithNumber(render: (FirBasedSymbol<*>) -> String): List<RenderedLineWithNumber> =
        render(this).lines().mapIndexed { index, line ->
            RenderedLineWithNumber(number = if (index == 0) toPhysicalLocation()?.line else null, renderedLine = line)
        }

    private inline fun SymbolNode.renderToLinesWithNumber(render: (FirBasedSymbol<*>) -> String): List<RenderedLineWithNumber> {
        val stack = mutableListOf(0 to this)
        var previousIndent = -1

        return buildList {
            while (stack.isNotEmpty()) {
                val [indent, element] = stack.popLast()
                val indentPadding = "    ".repeat(indent)

                if (indent < previousIndent) {
                    this += RenderedLineWithNumber(number = null, renderedLine = "$indentPadding}")
                }

                val ownLines = element.symbol.renderToLinesWithNumber(render).map { it.copy(renderedLine = indentPadding + it.renderedLine) }

                if (element.children.isNotEmpty()) {
                    this += ownLines.dropLast(1)
                    this += ownLines.last().copy(renderedLine = ownLines.last().renderedLine + " {")
                    stack += element.children.map { indent + 1 to it }
                } else {
                    this += ownLines
                }

                previousIndent = indent
            }

            if (previousIndent != 0) {
                this += RenderedLineWithNumber(number = null, renderedLine = "}")
            }
        }
    }

    private fun FirBasedSymbol<*>.collectContainers(): List<FirClassLikeSymbol<*>> =
        generateSequence(getContainingClassSymbol()) { it.getContainingClassSymbol() }.toList().reversed()

    private inline fun decorateCompleteLocation(
        logicalLocation: LogicalLocation,
        fileNode: FileNode,
        render: (FirBasedSymbol<*>) -> String,
    ): String {
        val renderedLines = fileNode.symbols.flatMap { it.renderToLinesWithNumber(render) }

        val longestNumberString = renderedLines.mapNotNull { it.number?.toString() }.maxByOrNull { it.length } ?: ""
        val indentString = " ".repeat(longestNumberString.length)
        val fileLine = "$indentString--> ${fileNode.filePath}"

        return renderLocationDecorationContents(
            logicalLocation, fileLine,
            renderedLines = renderedLines,
        )
    }

    private inline fun decorateIncompleteLocation(
        logicalLocation: LogicalLocation,
        symbols: Collection<SymbolNode>,
        render: (FirBasedSymbol<*>) -> String,
    ): String = renderLocationDecorationContents(
        logicalLocation, fileLine = null,
        renderedLines = symbols.flatMap { symbol -> symbol.renderToLinesWithNumber(render) }
    )

    private fun renderLocationDecorationContents(
        logicalLocation: LogicalLocation,
        fileLine: String?,
        renderedLines: List<RenderedLineWithNumber>,
    ): String {
        val longestNumberString = renderedLines.mapNotNull { it.number?.toString() }.maxByOrNull { it.length } ?: ""
        val indentString = " ".repeat(longestNumberString.length)

        val renderedLinesWithSeparators = renderedLines.interleaveWith { previous, next ->
            when {
                previous.number != null && previous.number + 1 == next.number -> null
                else -> RenderedLineWithNumber(number = null, renderedLine = "...")
            }
        }
        val paddedRenderedLines = renderedLinesWithSeparators.joinToString("\n") {
            val numberString = it.number?.toString()?.padStart(indentString.length, ' ') ?: indentString
            "$numberString | ${it.renderedLine}"
        }

        return renderLocationDecoration(logicalLocation, fileLine, indentString, paddedRenderedLines)
    }

    private inline fun <T> List<T>.interleaveWith(produceInterleave: (T, T) -> T?): Collection<T> {
        if (size <= 1) return this

        return buildList {
            add(this@interleaveWith.first())

            for (it in 1 until this@interleaveWith.size) {
                produceInterleave(this@interleaveWith[it - 1], this@interleaveWith[it])?.let(::add)
                add(this@interleaveWith[it])
            }
        }
    }

    private fun renderLocationDecoration(
        logicalLocation: LogicalLocation,
        fileLine: String?,
        indentString: String,
        content: String,
    ): String = listOfNotNull(
        "In package '${logicalLocation.packageName}' of module '${logicalLocation.moduleName}':",
        fileLine,
        "$indentString |",
        content,
        "$indentString |",
    ).joinToString("\n")

    private fun FirBasedSymbol<*>.toLogicalLocation(): LogicalLocation {
        if (this is FirTypeParameterSymbol) return containingDeclarationSymbol.toLogicalLocation()
        val packageName = if (packageFqName().isRoot) "root package" else packageFqName().render()
        val moduleName = moduleData.name
        return LogicalLocation(packageName, moduleName)
    }

    private data class LogicalLocation(
        val packageName: String,
        val moduleName: Name,
    )

    private fun FirBasedSymbol<*>.toPhysicalLocation(): PhysicalLocation? {
        val filePath = source?.let(moduleData.session.sourcesToPathsMapper::getRelativeSourceFilePath) ?: return null
        val [line, column] = source?.let { it.toLinesMapping().getLineAndColumnByOffset(it.startOffset) }
            ?.let { [line, column] -> (line + 1) to (column + 1) }
            ?: return null
        return PhysicalLocation(filePath, line, column)
    }

    private data class PhysicalLocation(
        val filePath: String,
        val line: Int,
        val column: Int,
    )

    @OptIn(SymbolInternals::class)
    val TYPE_PARAMETER_OWNER_SYMBOL = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirClassLikeSymbol, is FirCallableSymbol -> FirRenderer(
                typeRenderer = ConeTypeRendererForReadability { ConeIdShortRenderer() },
                idRenderer = ConeIdShortRenderer(),
                classMemberRenderer = FirNoClassMemberRenderer(),
                bodyRenderer = null,
                propertyAccessorRenderer = null,
                callArgumentsRenderer = FirCallNoArgumentsRenderer(),
                modifierRenderer = null,
                callableSignatureRenderer = null,
                declarationRenderer = FirDeclarationRenderer("local "),
                annotationRenderer = null,
                contractRenderer = null,
                supertypeRenderer = null,
                lineBreakAfterContextParameters = false,
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
        "\n" + symbols.renderWithLocationInGroups(SYMBOL::render)
    }

    /**
     * Prepends [singular] or [plural] depending on the elements count.
     */
    fun <Q> prefix(
        singular: String,
        plural: String,
        renderer: ContextIndependentParameterRenderer<Collection<Q>>,
    ): ContextIndependentParameterRenderer<Collection<Q>> {
        return Renderer { elements ->
            val decoration = if (elements.size == 1) singular else plural
            decoration + renderer.render(elements)
        }
    }

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

    val CALLABLE_FQ_NAME = Renderer { symbol: FirCallableSymbol<*> ->
        val origin = symbol.containingClassLookupTag()?.classId?.asFqNameString()
        SYMBOL.render(symbol) + origin?.let { ", defined in $it" }.orEmpty()
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

    val RENDER_TYPE = ContextDependentRenderer { t: ConeKotlinType, ctx ->
        // TODO, KT-59811: need a way to tune granuality, e.g., without parameter names in functional types.
        ctx[FirAdaptiveTypeRenderingKey].getValue(t)
    }

    val RENDER_FQ_NAME_WITH_PREFIX = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirCallableSymbol<*> -> CALLABLE_FQ_NAME.render(symbol)
            is FirClassLikeSymbol<*> -> RENDER_CLASS_OR_OBJECT(quoted = false) { classId -> classId.asFqNameString() }.render(symbol)
            else -> return@Renderer "???"
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

    val FOR_OPTIONAL_RECEIVER = ContextDependentRenderer { receiverInfo: ConeReceiverInfo?, ctx ->
        if (receiverInfo == null || receiverInfo.type.hasError()) return@ContextDependentRenderer ""
        buildString {
            if (receiverInfo.couldBePositionalDestructuring) {
                append(" for name-based destructuring")
            }
            append(" on receiver of type '")
            append(RENDER_TYPE.render(receiverInfo.type, ctx))
            append("'")

            if (receiverInfo.couldBePositionalDestructuring) {
                append(". To use positional destructuring, use the 'val [...] = ...' syntax")
            }
        }
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

    val SYMBOL_WITH_CONTAINING_DECLARATION = Renderer { symbol: FirBasedSymbol<*> ->
        val containingClassId = when (symbol) {
            is FirCallableSymbol<*> -> symbol.callableId?.classId
            is FirTypeParameterSymbol -> (symbol.containingDeclarationSymbol as? FirClassLikeSymbol<*>)?.classId
            else -> null
        } ?: return@Renderer "'${SYMBOL.render(symbol)}'"
        "'${SYMBOL.render(symbol)}' defined in ${NAME_OF_DECLARATION_OR_FILE.render(containingClassId)}"
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

    val CANDIDATES_WITH_DIAGNOSTIC_MESSAGES = Renderer { list: Collection<Pair<FirBasedSymbol<*>, List<String>>> ->
        buildString {
            for ([symbol, diagnostics] in list) {
                append(SYMBOL_WITH_LOCATION.render(symbol))

                if (diagnostics.isNotEmpty()) {
                    if (diagnostics.size == 1) {
                        append("\nreason: ")
                        appendLine(diagnostics.single())
                    } else {
                        appendLine("\nreasons:")
                        diagnostics.forEach {
                            append("  - ")
                            appendLine(it)
                        }
                    }
                }

                appendLine()
            }
        }.trim()
    }

    val DEPRECATING_FEATURE = Renderer { feature: LanguageFeature ->
        buildString {
            appendDeprecationWarningSuffix(feature)
        }
    }

    private enum class UnsafeKind {
        Nullable, Union, NullableUnion,
    }

    private object FirUnsafeKindKey : RenderingContext.Key<Map<ConeKotlinType, UnsafeKind>>("UNSAFE_KIND") {
        override fun compute(
            objectsToRender: Collection<Any?>,
            diagnosticContext: DiagnosticBaseContext,
        ): Map<ConeKotlinType, UnsafeKind> = context(diagnosticContext as SessionHolder) {
            objectsToRender
                .filterIsInstance<ConeKotlinType>()
                .associateWith {
                    val canBeNull = it.canBeNull()
                    val isUnion = it is ConeUnionType
                    when {
                        canBeNull && isUnion -> UnsafeKind.NullableUnion
                        canBeNull -> UnsafeKind.Nullable
                        else -> UnsafeKind.Union
                    }
                }
        }
    }

    private fun unsafeCallRenderer(suffix: (UnsafeKind, String) -> String): DiagnosticParameterRenderer<ConeKotlinType> {
        return ContextDependentRenderer { type: ConeKotlinType, context ->
            val kind = context[FirUnsafeKindKey].getValue(type)
            val renderedType = RENDER_TYPE.render(type, context)
            "on receiver of " + suffix(kind, renderedType)
        }
    }

    val UNSAFE_CALL_RECEIVER = unsafeCallRenderer { kind, renderedType ->
        when (kind) {
            Nullable -> "nullable type '$renderedType'"
            Union -> "union type '$renderedType'"
            NullableUnion -> "nullable union type '$renderedType'"
        }
    }

    val UNSAFE_CALL_RECEIVER_WITH_HINT = unsafeCallRenderer { kind, renderedType ->
        when (kind) {
            Nullable -> "nullable type '$renderedType'. Consider using a safe call '?.' or non-null asserted call '!!.'"
            Union -> "union type '$renderedType'. Consider using an error-safe call '|.'"
            NullableUnion -> "nullable union type '$renderedType'"
        }
    }

    val UNSAFE_INVOKE_CALL_RECEIVER_WITH_HINT = unsafeCallRenderer { kind, renderedType ->
        when (kind) {
            Nullable -> "nullable type '$renderedType'. Consider using a safe call '?.invoke' or non-null asserted call '!!()'"
            Union -> "union type '$renderedType'. Consider using an error-safe call '|.invoke'"
            NullableUnion -> "nullable union type '$renderedType'"
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
