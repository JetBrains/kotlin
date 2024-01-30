/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dump

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.apache.commons.lang.StringEscapeUtils
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.left
import org.jetbrains.kotlin.fir.backend.right
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintError
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import java.io.File
import java.io.Writer


internal interface FirLinkResolver {
    fun nearPackage(fqName: FqName): String?

    fun symbolSignature(symbol: FirBasedSymbol<*>): String
    fun nearSymbolLocation(symbol: FirBasedSymbol<*>): String?

    fun classLocation(classId: ClassId): String?

    fun HEAD.supplementary()
}


private const val PACKAGE_INDEX = "package-index.html"
private const val MODULE_INDEX = "module-index.html"
private const val ROOT_INDEX = "root-index.html"


private fun HEAD.commonHead() {
    meta { charset = "utf-8" }
    script(src = "https://code.jquery.com/jquery-3.4.0.slim.min.js") {
        integrity = "sha256-ZaXnYkHGqIhqTbJ6MB4l9Frs/r7U4jlx7ir8PJYBqbI="
        @Suppress("SpellCheckingInspection")
        attributes["crossorigin"] = "anonymous"
    }
}


private class PackageInfo(val fqName: FqName, val moduleInfo: ModuleInfo) {
    val contents = LinkedHashSet<String>()
    val packageRoot = fqName.pathSegments().fold(moduleInfo.moduleRoot) { dir, segment -> dir.resolve(segment.asString()) }.also {
        it.mkdirs()
    }
    val errors = mutableMapOf<String, Int>().withDefault { 0 }
    val implicits = mutableMapOf<String, Int>().withDefault { 0 }
    val unresolved = mutableMapOf<String, Int>().withDefault { 0 }
}

private class ModuleInfo(val name: String, outputRoot: File) {
    val packages = mutableMapOf<FqName, PackageInfo>()
    val moduleRoot = outputRoot.resolve(name).also {
        it.mkdirs()
    }
    val errors: Map<FqName, Int> by lazy {
        packages.mapValues { (_, packageInfo) -> packageInfo.errors.values.sum() }.withDefault { 0 }
    }
    val implicits: Map<FqName, Int> by lazy {
        packages.mapValues { (_, packageInfo) -> packageInfo.implicits.values.sum() }.withDefault { 0 }
    }
    val unresolved: Map<FqName, Int> by lazy {
        packages.mapValues { (_, packageInfo) -> packageInfo.unresolved.values.sum() }.withDefault { 0 }
    }
}

private class SupplementaryGenerator(val outputRoot: File) {
    fun generateIndex(moduleInfo: ModuleInfo, writer: Writer) {
        writer.appendHTML().html {
            head {
                title { +moduleInfo.name }
                commonHead()
                supplementary(this, moduleInfo.moduleRoot)
            }
            body {
                h4 {
                    a(
                        href = linkToRootIndex(moduleInfo.moduleRoot),
                        classes = "container-ref"
                    ) {
                        +"back to root"
                    }
                }
                h2 { +moduleInfo.name }
                ul {
                    for (packageInfo in moduleInfo.packages.values) {
                        li {
                            a(
                                href = linkToIndex(packageInfo, moduleInfo.moduleRoot),
                                classes = "container-ref"
                            ) {
                                +packageInfo.fqName.asString()
                            }
                            addErrors(
                                moduleInfo.errors.getValue(packageInfo.fqName),
                                moduleInfo.implicits.getValue(packageInfo.fqName),
                                moduleInfo.unresolved.getValue(packageInfo.fqName)
                            )
                        }
                    }
                }
            }
        }
    }

    fun LI.addErrors(errors: Int, implicits: Int, unresolved: Int) {
        if (errors > 0) {
            span(classes = "error-counter") { +(errors.toString()) }
        }
        if (implicits > 0) {
            span(classes = "implicit-counter") { +(implicits.toString()) }
        }
        if (unresolved > 0) {
            span(classes = "unresolved-counter") { +(unresolved.toString()) }
        }
    }

    fun linkToIndex(moduleInfo: ModuleInfo, from: File): String {
        return moduleInfo.moduleRoot.resolve(MODULE_INDEX).relativeTo(from).path
    }

    fun linkToIndex(packageInfo: PackageInfo, from: File): String {
        return packageInfo.packageRoot.resolve(PACKAGE_INDEX).relativeTo(from).path
    }

    fun linkToRootIndex(from: File): String {
        return outputRoot.resolve(ROOT_INDEX).relativeTo(from).path
    }

    fun generateIndex(packageInfo: PackageInfo, writer: Writer) {
        writer.appendHTML().html {
            head {
                title { +packageInfo.fqName.asString() }
                commonHead()
                supplementary(this, packageInfo.packageRoot)
            }
            body {
                h4 {
                    +"In module: "
                    a(
                        href = linkToIndex(packageInfo.moduleInfo, packageInfo.packageRoot),
                        classes = "container-ref"
                    ) {
                        +packageInfo.moduleInfo.name
                    }
                }
                h2 { +packageInfo.fqName.asString() }

                ul {
                    for (file in packageInfo.contents) {
                        li {
                            a(href = "./$file.fir.html", classes = "container-ref") { +file }
                            addErrors(
                                packageInfo.errors.getValue(file),
                                packageInfo.implicits.getValue(file),
                                packageInfo.unresolved.getValue(file)
                            )
                        }
                    }
                }
            }
        }
    }

    fun generateIndex(modules: List<ModuleInfo>, writer: Writer) {
        val title = "Root dump index"
        writer.appendHTML().html {
            head {
                title { +title }
                commonHead()
                supplementary(this, outputRoot)
            }
            body {
                h2 { +title }

                ul {
                    for (module in modules) {
                        li {
                            a(href = linkToIndex(module, outputRoot), classes = "container-ref") { +module.name }
                            addErrors(
                                module.errors.values.sum(),
                                module.implicits.values.sum(),
                                module.unresolved.values.sum()
                            )
                        }
                    }
                }
            }
        }
    }


    fun supplementary(head: HEAD, originDir: File) = with(head) {
        for (file in jsFiles) {
            script(src = outputRoot.resolve(file).relativeTo(originDir).path) {}
        }
        for (styleSheet in cssFiles) {
            styleLink(outputRoot.resolve(styleSheet).relativeTo(originDir).path)
        }
    }
}


private val jsFiles = listOf("logic.js")
private val cssFiles = listOf("style.css", "colors.white.css", "colors.dark.css")


class MultiModuleHtmlFirDump(private val outputRoot: File) {

    private val modules = LinkedHashMap<String, ModuleInfo>()

    private lateinit var currentModule: ModuleInfo
    private var inModule = false
    private var finished = false
    private var index = Index()

    fun module(module: String, block: () -> Unit) {
        require(!finished)
        inModule = true
        currentModule = modules.getOrPut(module) {
            ModuleInfo(module, outputRoot)
        }

        block()
        inModule = false
        index = Index()
    }

    private val supplementaryGenerator = SupplementaryGenerator(outputRoot)

    fun finish() {
        require(!finished)
        finished = true
        outputRoot.resolve(ROOT_INDEX).writer().use {
            supplementaryGenerator.generateIndex(modules.values.toList(), it)
        }
        for (module in modules.values) {
            module.moduleRoot.resolve(MODULE_INDEX).writer().use {
                supplementaryGenerator.generateIndex(module, it)
            }
            for (packageInfo in module.packages.values) {
                packageInfo.packageRoot.resolve(PACKAGE_INDEX).writer().use {
                    supplementaryGenerator.generateIndex(packageInfo, it)
                }
            }
        }
        val supplementaryFiles = jsFiles + cssFiles

        for (file in supplementaryFiles) {
            val stream = this::class.java.getResourceAsStream(file)
            outputRoot.resolve(file).outputStream().use { outputStream ->
                stream.copyTo(outputStream)
            }
        }
    }

    private fun locationForFile(firFile: FirFile, packageInfo: PackageInfo = packageForFile(firFile)): File {
        var name = firFile.name
        var index = 0
        while (!packageInfo.contents.add(name)) {
            name = firFile.name + ".${index++}"
        }
        return packageInfo.packageRoot.resolve("$name.fir.html")
    }

    private fun packageForFile(firFile: FirFile): PackageInfo {
        val packageFqName = firFile.packageFqName
        return currentModule.packages.getOrPut(packageFqName) {
            PackageInfo(packageFqName, currentModule)
        }
    }

    fun indexFile(file: FirFile) {
        val location = locationForFile(file)
        index.files[file] = location
        file.accept(index.visitor(location))
    }

    fun generateFile(file: FirFile) {
        require(inModule)

        val dumpOutput = index.files[file] ?: error("No location for ${file.name}")
        val dumper = HtmlFirDump(LinkResolver(dumpOutput), file.moduleData.session)
        val builder = StringBuilder()
        dumper.generate(file, builder)

        dumpOutput.writeText(builder.toString())
        packageForFile(file).apply {
            errors[file.name] = dumper.errors
            implicits[file.name] = dumper.implicits
            unresolved[file.name] = dumper.unresolved
        }
    }


    private inner class LinkResolver(origin: File) : FirLinkResolver {

        override fun HEAD.supplementary() {
            supplementaryGenerator.supplementary(this, originDir)
        }

        override fun symbolSignature(symbol: FirBasedSymbol<*>): String {
            val id = index.symbolIds[symbol] ?: error("Not found $symbol")
            return "id$id"
        }

        private val originDir = origin.parentFile

        override fun nearPackage(fqName: FqName): String? {
            val packageInfo = currentModule.packages[fqName] ?: return null
            return packageInfo.packageRoot.resolve(PACKAGE_INDEX).relativeTo(originDir).path
        }

        override fun classLocation(classId: ClassId): String? {
            val location = index.classes[classId] ?: return null
            return location.relativeTo(originDir).path + "#$classId"
        }

        override fun nearSymbolLocation(symbol: FirBasedSymbol<*>): String? {
            val location = index.symbols[symbol] ?: return null
            return location.relativeTo(originDir).path + "#${symbolSignature(symbol)}"
        }
    }

    private inner class Index {
        val files = mutableMapOf<FirFile, File>()
        val classes = mutableMapOf<ClassId, File>()
        val symbols = mutableMapOf<FirBasedSymbol<*>, File>()
        val symbolIds = mutableMapOf<FirBasedSymbol<*>, Int>()
        private var symbolCounter = 0

        fun visitor(location: File): FirVisitorVoid {
            return object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    element.acceptChildren(this)
                }

                override fun visitRegularClass(regularClass: FirRegularClass) {
                    classes[regularClass.classId] = location
                    visitElement(regularClass)
                }

                fun indexDeclaration(declaration: FirDeclaration) {
                    symbols[declaration.symbol] = location
                    symbolIds[declaration.symbol] = symbolCounter++
                }

                override fun visitVariable(variable: FirVariable) {
                    indexDeclaration(variable)
                    visitElement(variable)
                }

                override fun visitValueParameter(valueParameter: FirValueParameter) {
                    indexDeclaration(valueParameter)
                    visitElement(valueParameter)
                }

                override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                    indexDeclaration(simpleFunction)
                    visitElement(simpleFunction)
                }

                override fun visitTypeParameter(typeParameter: FirTypeParameter) {
                    indexDeclaration(typeParameter)
                    visitElement(typeParameter)
                }

                override fun visitProperty(property: FirProperty) {
                    indexDeclaration(property)
                    visitElement(property)
                }

                override fun visitConstructor(constructor: FirConstructor) {
                    indexDeclaration(constructor)
                    visitElement(constructor)
                }
            }
        }

    }
}

class HtmlFirDump internal constructor(private var linkResolver: FirLinkResolver, private val session: FirSession) {
    var errors: Int = 0
        private set

    var implicits: Int = 0
        private set

    var unresolved: Int = 0
        private set

    fun generate(element: FirFile, builder: StringBuilder) {
        errors = 0
        implicits = 0
        unresolved = 0
        builder.appendHTML().html {
            generate(element)
        }
    }

    private fun FlowContent.keyword(text: String) {
        span(classes = "keyword") { +text }
    }

    private fun FlowContent.line(block: FlowContent.() -> Unit) {
        nl()
        block()
        br
    }

    private fun FlowContent.nl(block: FlowContent.() -> Unit = {}) {
        span(classes = "line") { block() }
    }

    private fun FlowContent.inl() {
        nl()
        ident()
    }

    private fun FlowContent.simpleName(name: Name) {
        span(classes = "simple-name") {
            +name.asString()
        }
    }

    private fun FlowContent.unresolved(block: FlowContent.() -> Unit) {
        span(classes = "unresolved") {
            block()
        }
    }

    private fun FlowContent.resolved(block: FlowContent.() -> Unit) {
        span(classes = "resolved") {
            block()
        }
    }

    private fun FlowContent.error(block: SPAN.() -> Unit) {
        errors++
        span(classes = "error") {
            block()
        }
    }

    private fun FlowContent.packageName(fqName: FqName) {
        line {
            keyword("package")
            ws
            val link = linkResolver.nearPackage(fqName)!!
            a(href = link, classes = "package-fqn") { +fqName.asString() }
        }
    }

    private val FlowContent.ws get() = +" "

    private fun FlowContent.fqn(name: FqName) {
        +name.asString()
    }

    private fun FlowContent.inlineUnsupported(element: Any) {
        span(classes = "fold-container") {
            span(classes = "error unsupported") {
                +"Unsupported: ${element::class}"
            }
            span("fold-region") {
                val content = when (element) {
                    is FirElement -> element.render()
                    else -> element.toString()
                }
                +"\n"
                +content
                +"\n"
            }
        }
    }

    private fun FlowContent.unsupported(element: FirElement) {
        line {
            inlineUnsupported(element)
        }
    }

    private fun FlowContent.modality(modality: Modality?) {
        if (modality == null) return
        keyword(modality.name.lowercase())
    }

    private fun FlowContent.visibility(visibility: Visibility) {
        if (visibility == Visibilities.Unknown)
            return unresolved { keyword("public?") }
        return keyword(visibility.toString())
    }

    private fun FlowContent.declarationStatus(status: FirDeclarationStatus) {
        visibility(status.visibility)
        ws
        modality(status.modality)
        ws
        if (status.isExpect) {
            keyword("expect ")
        }
        if (status.isActual) {
            keyword("actual ")
        }
        if (status.isOverride) {
            keyword("override ")
        }
        if (status.isInner) {
            keyword("inner ")
        }
        if (status.isCompanion) {
            keyword("companion ")
        }
        if (status.isInline) {
            keyword("inline ")
        }
        if (status.isInfix) {
            keyword("infix ")
        }
        if (status.isExternal) {
            keyword("external ")
        }
        if (status.isTailRec) {
            keyword("tailrec ")
        }
        if (status.isOperator) {
            keyword("operator ")
        }
        if (status.isConst) {
            keyword("const ")
        }
        if (status.isLateInit) {
            keyword("lateinit ")
        }
        if (status.isData) {
            keyword("data ")
        }
        if (status.isSuspend) {
            keyword("suspend ")
        }
        if (status.isStatic) {
            keyword("static ")
        }
    }


    private fun FlowContent.anchoredName(name: Name, signature: String) {
        span(classes = "declaration") {
            id = signature
            simpleName(name)
        }
    }

    private var currentIdent = 0

    private fun FlowContent.ident(level: Int = currentIdent) {
        +" ".repeat(level * 4)
    }

    @Suppress("SpellCheckingInspection")
    private fun FlowContent.iline(block: FlowContent.() -> Unit) {
        inl()
        block()
        br
    }

    private fun FlowContent.withIdentLevel(block: FlowContent.() -> Unit) {
        currentIdent++
        block()
        currentIdent--
    }

    private fun <E> FlowContent.generateList(list: List<E>, separator: String = ", ", generate: FlowContent.(E) -> Unit) {
        if (list.isEmpty()) return
        generate(list.first())
        for (element in list.drop(1)) {
            +separator
            generate(element)
        }
    }

    private fun FlowContent.classKind(kind: ClassKind) {
        when (kind) {
            ClassKind.CLASS -> keyword("class")
            ClassKind.INTERFACE -> keyword("interface")
            ClassKind.ENUM_CLASS -> keyword("enum class")
            ClassKind.ENUM_ENTRY -> Unit // ?
            ClassKind.ANNOTATION_CLASS -> keyword("annotation class")
            ClassKind.OBJECT -> keyword("object")
        }
    }

    private fun FlowContent.generate(klass: FirRegularClass) {
        inl()

        declarationStatus(klass.status)
        classKind(klass.classKind)
        ws
        anchoredName(klass.name, klass.classId.asString())
        generateTypeParameters(klass)
        if (klass.superTypeRefs.isNotEmpty()) {
            +": "
            generateList(klass.superTypeRefs) {
                generate(it)
            }
        }

        generateDeclarations(klass.declarations)
        br

    }

    private fun FlowContent.generate(typeAlias: FirTypeAlias) {
        inl()

        declarationStatus(typeAlias.status)

        keyword("typealias")
        ws
        anchoredName(typeAlias.name, typeAlias.symbol.classId.asString())
        generateTypeParameters(typeAlias)

        +" = "

        val type = typeAlias.expandedConeType
        if (type != null) {
            generate(type as ConeKotlinType)
        } else {
            +"<error expanded type>"
        }

        br
    }

    private fun FlowContent.generate(flexibleType: ConeFlexibleType) {
        if (flexibleType.lowerBound.nullability == ConeNullability.NOT_NULL &&
            flexibleType.upperBound.nullability == ConeNullability.NULLABLE &&
            AbstractStrictEqualityTypeChecker.strictEqualTypes(
                session.typeContext,
                flexibleType.lowerBound,
                flexibleType.upperBound.withNullability(ConeNullability.NOT_NULL, session.typeContext)
            )
        ) {
            generate(flexibleType.lowerBound)
            +"!"
        } else {
            generate(flexibleType.lowerBound)
            +".."
            generate(flexibleType.upperBound)
        }
    }

    private fun FlowContent.generate(intersectionType: ConeIntersectionType) {
        +"("
        generateList(intersectionType.intersectedTypes.toList(), " & ") { generate(it) }
        +")"
    }

    private fun FlowContent.generate(type: ConeClassLikeType) {
        resolved {
            when (val symbol = type.lookupTag.toSymbol(session)) {
                is FirTypeAliasSymbol -> {
                    symbolRef(symbol) {
                        simpleName(type.lookupTag.name)
                    }
                    generateTypeArguments(type)
                    if (type.isMarkedNullable) {
                        +"?"
                    }
                    +" = "
                    val directlyExpanded = type.directExpansionType(session)
                    if (directlyExpanded != null) {
                        generate(directlyExpanded.fullyExpandedType(session))
                    } else {
                        error { +"No expansion for type-alias" }
                    }
                }
                else -> {
                    symbolRef(symbol) {
                        fqn(type.lookupTag.classId.relativeClassName)
                    }
                    generateTypeArguments(type)
                    if (type.isMarkedNullable) {
                        +"?"
                    }
                }
            }
        }
    }

    private fun FlowContent.generate(variableAssignment: FirVariableAssignment) {
        generate(variableAssignment.lValue)
        +" = "
        generate(variableAssignment.rValue)
    }

    private fun FlowContent.generate(projection: ConeTypeProjection) {
        when (projection) {
            is ConeStarProjection -> +"*"
            is ConeKotlinTypeProjection -> {
                when (projection.kind) {
                    ProjectionKind.IN -> keyword("in ")
                    ProjectionKind.OUT -> keyword("out ")
                    else -> {
                    }
                }

                generate(projection.type)
            }
        }
    }

    private fun FlowContent.stringLiteral(value: Any?) {
        val text = StringEscapeUtils.escapeJava(value.toString())
        span("string-literal") {
            when (value) {
                is String -> +"\"$text\""
                is Char -> +"'$text'"
                else -> error("Unknown string literal: \"$value\" ${value?.let { it::class }}")
            }
        }
    }

    private fun FlowContent.generate(expression: FirLiteralExpression<*>) {
        val value = expression.value
        if (value == null && expression.kind != ConstantValueKind.Null) {
            return error {
                +"null value"
            }
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        when (expression.kind) {
            ConstantValueKind.Null -> keyword("null")
            ConstantValueKind.Boolean -> keyword(value.toString())
            ConstantValueKind.String, ConstantValueKind.Char ->
                stringLiteral(value)
            ConstantValueKind.Byte -> {
                +value.toString()
                keyword("B")
            }
            ConstantValueKind.Short -> {
                +value.toString()
                keyword("S")
            }
            ConstantValueKind.Int -> {
                +value.toString()
                keyword("I")
            }
            ConstantValueKind.Long -> {
                +value.toString()
                keyword("L")
            }
            ConstantValueKind.UnsignedByte -> {
                +(value as Long).toUByte().toString()
                keyword("uB")
            }
            ConstantValueKind.UnsignedShort -> {
                +(value as Long).toUShort().toString()
                keyword("uS")
            }
            ConstantValueKind.UnsignedInt -> {
                +(value as Long).toUInt().toString()
                keyword("uI")
            }
            ConstantValueKind.UnsignedLong -> {
                +(value as Long).toULong().toString()
                keyword("uL")
            }
            ConstantValueKind.Float -> {
                +value.toString()
                keyword("F")
            }
            ConstantValueKind.Double -> {
                +value.toString()
                keyword("D")
            }
            ConstantValueKind.IntegerLiteral -> {
                +"IL<"
                +value.toString()
                +">"
            }
            ConstantValueKind.UnsignedIntegerLiteral -> {
                +"UIL<"
                +value.toString()
                +">"
            }
            ConstantValueKind.Error -> {
                +"ERROR_CONSTANT"
            }
        }

    }

    private fun FlowContent.generateTypeArguments(type: ConeKotlinType) {
        if (type.typeArguments.isNotEmpty()) {
            +"<"
            generateList(type.typeArguments.toList()) {
                generate(it)
            }
            +">"
        }
    }

    private fun FlowContent.generate(type: ConeKotlinType) {
        when (type) {
            is ConeErrorType -> error { +type.diagnostic.reason }
            is ConeClassLikeType -> return generate(type)
            is ConeTypeParameterType -> resolved {
                symbolRef(type.lookupTag.symbol) {
                    simpleName(type.lookupTag.name)
                }
            }
            is ConeTypeVariableType -> resolved { +type.typeConstructor.name.asString() }
            is ConeFlexibleType -> resolved { generate(type) }
            is ConeCapturedType -> inlineUnsupported(type)
            is ConeDefinitelyNotNullType -> resolved {
                generate(type.original)
                +" & Any"
            }
            is ConeIntersectionType -> resolved { generate(type) }
            is ConeIntegerLiteralType -> inlineUnsupported(type)
            is ConeLookupTagBasedType,
            is ConeStubType -> {}
        }
        generateTypeArguments(type)
        if (type.isMarkedNullable) {
            +"?"
        }
    }

    private fun FlowContent.generate(typeProjection: FirTypeProjection) {
        when (typeProjection) {
            is FirTypeProjectionWithVariance -> {
                generate(typeProjection.variance)
                generate(typeProjection.typeRef)
            }
            is FirStarProjection -> +"*"
            is FirPlaceholderProjection -> +"_"
        }
    }

    private fun FlowContent.generate(variance: Variance) {
        when (variance) {
            Variance.INVARIANT -> Unit
            Variance.IN_VARIANCE -> keyword("in ")
            Variance.OUT_VARIANCE -> keyword("out ")
        }
    }

    private fun FlowContent.generateTypeProjections(typeProjections: List<FirTypeProjection>) {
        if (typeProjections.isEmpty()) return
        +"<"
        generateList(typeProjections) {
            generate(it)
        }
        +">"
    }

    private fun FlowContent.generate(typeRef: FirTypeRef) {
        when (typeRef) {
            is FirErrorTypeRef -> error { +typeRef.diagnostic.reason }
            is FirResolvedTypeRef -> generate(typeRef.type)
            is FirImplicitTypeRef -> unresolved {
                implicits++
                keyword("<implicit>")
            }
            is FirUserTypeRef -> unresolved {
                unresolved++
                generateList(typeRef.qualifier, separator = ".") {
                    simpleName(it.name)
                    generateTypeProjections(it.typeArgumentList.typeArguments)
                }
                if (typeRef.isMarkedNullable) {
                    +"?"
                }
            }
            else -> inlineUnsupported(typeRef)
        }
    }

    private fun FlowContent.generate(memberDeclaration: FirMemberDeclaration) {
        when (memberDeclaration) {
            is FirEnumEntry -> generate(memberDeclaration)
            is FirRegularClass -> generate(memberDeclaration)
            is FirSimpleFunction -> generate(memberDeclaration)
            is FirProperty -> if (memberDeclaration.isLocal) generate(memberDeclaration as FirVariable) else generate(memberDeclaration)
            is FirConstructor -> generate(memberDeclaration)
            is FirTypeAlias -> generate(memberDeclaration)
            else -> unsupported(memberDeclaration)
        }
    }

    private fun FlowContent.generateTypeParameters(typeParameterContainer: FirTypeParameterRefsOwner, describe: Boolean = false) {
        if (typeParameterContainer.typeParameters.isEmpty()) return
        +"<"
        fun generateTypeParameter(typeParameter: FirTypeParameter, describe: Boolean) {
            generate(typeParameter.variance)
            if (typeParameter.isReified) {
                keyword("reified ")
            }
            if (describe)
                symbolRef(typeParameter.symbol) {
                    simpleName(typeParameter.name)
                }
            else
                symbolAnchor(typeParameter.symbol) {
                    simpleName(typeParameter.name)
                }
            if (typeParameter.bounds.isNotEmpty()) {
                +": "
                generateList(typeParameter.bounds) { bound ->
                    generate(bound)
                }
            }
        }
        generateList(typeParameterContainer.typeParameters) {
            if (it is FirTypeParameter) {
                generateTypeParameter(it, describe)
            } else {
                symbolRef(it.symbol) {
                    +"^"
                    simpleName(it.symbol.fir.name)
                }
            }
        }
        +"> "
    }

    private fun FlowContent.generateReceiver(declaration: FirCallableDeclaration) {
        generateReceiver(declaration.receiverParameter)
    }

    private fun FlowContent.generateReceiver(receiverParameter: FirReceiverParameter?) {
        receiverParameter ?: return
        generate(receiverParameter.typeRef)
        +"."
    }

    private fun FlowContent.generate(accessor: FirPropertyAccessor) {
        if (accessor is FirDefaultPropertyAccessor) return
        iline {
            declarationStatus(accessor.status)
            if (accessor.isGetter) {
                keyword("get")
            } else if (accessor.isSetter) {
                keyword("set")
            }
            +"("
            generateList(accessor.valueParameters) {
                generate(it)
            }
            +")"
            generateBlockIfAny(accessor.body)
        }
    }

    private fun FlowContent.generate(property: FirProperty) {
        //anchor
        iline {
            declarationStatus(property.status)
            if (property.isVal) {
                keyword("val ")
            } else {
                keyword("var ")
            }
            generateTypeParameters(property)
            generateReceiver(property)
            symbolAnchor(property.symbol) {
                simpleName(property.name)
            }
            +": "
            generate(property.returnTypeRef)


            val initializer = property.initializer
            if (initializer != null) {
                +" = "
                generate(initializer)
            }

            val delegate = property.delegate
            if (delegate != null) {
                keyword(" by ")
                generate(delegate)
            }
        }

        withIdentLevel {
            property.getter?.let { generate(it) }
            property.setter?.let { generate(it) }
        }
    }


    private fun FlowContent.exprType(type: FirTypeRef, block: SPAN.() -> Unit) {
        span(classes = "typed-expression fold-container") {
            block()
            span(classes = "expression-type fold-region") {
                +": "
                generate(type)
            }
        }
    }

    private fun FlowContent.generate(statement: FirStatement) {
        when (statement) {
            is FirSimpleFunction -> generate(statement)
            is FirAnonymousObject -> generate(statement, isStatement = true)
            is FirAnonymousFunction -> generate(statement, isStatement = true)
            is FirWhileLoop -> generate(statement)
            is FirWhenExpression -> generate(statement, isStatement = true)
            is FirTryExpression -> generate(statement, isStatement = true)
            is FirExpression -> iline { generate(statement) }
            is FirVariable -> iline { generate(statement) }
            is FirVariableAssignment -> iline { generate(statement) }
            else -> unsupported(statement)
        }
    }

    private fun FlowContent.generate(delegatedConstructorCall: FirDelegatedConstructorCall) {
        generateMultiLineExpression(isStatement = true) {
            iline {
                exprType(delegatedConstructorCall.constructedTypeRef) {
                    if (delegatedConstructorCall.isSuper) keyword("super")
                    if (delegatedConstructorCall.isThis) keyword("this")
                    +"<"
                    generate(delegatedConstructorCall.calleeReference)
                    +">"
                    +"("
                    generateList(delegatedConstructorCall.arguments) {
                        generate(it)
                    }
                    +")"
                }
            }
        }
    }

    private fun FlowContent.generate(variable: FirVariable) {
        if (variable.isVal) {
            keyword("val ")
        } else {
            keyword("var ")
        }

        symbolAnchor(variable.symbol) { simpleName(variable.name) }
        +": "
        generate(variable.returnTypeRef)
        val initializer = variable.initializer
        if (initializer != null) {
            +" = "
            generate(initializer)
        }
    }

    private fun FlowContent.declarationRef(
        href: String?,
        classes: Set<String> = emptySet(),
        body: A.() -> Unit
    ) {
        a(href = href, classes = "ref") {
            this.classes += classes
            if (href == null) {
                this.classes += "external"
            }
            body()
        }
    }

    private fun FirBasedSymbol<*>.describe(): String {
        return when (this) {
            is FirClassLikeSymbol<*> -> classId.asString()
            is FirCallableSymbol<*> -> callableId.toString()
            is FirTypeParameterSymbol -> name.asString()
            else -> ""
        }
    }

    private fun FlowContent.describeVerbose(symbol: FirCallableSymbol<*>, fir: FirFunction) {
        describeTypeParameters(fir)

        fir.receiverParameter?.typeRef?.let {
            +"("
            generate(it)
            +")."
        }
        symbolRef(symbol) {
            +symbol.callableId.toString()
        }
        +"("
        generateList(fir.valueParameters) {
            generate(it.returnTypeRef)
        }
        +"): "
        generate(fir.returnTypeRef)
    }

    private fun FlowContent.describeVerbose(symbol: FirCallableSymbol<*>, fir: FirVariable) {
        if (fir is FirTypeParametersOwner) describeTypeParameters(fir)

        fir.receiverParameter?.typeRef?.let {
            +"("
            generate(it)
            +")."
        }
        symbolRef(symbol) {
            +symbol.callableId.toString()
        }
        +":"
        generate(fir.returnTypeRef)
    }

    private fun FlowContent.describeTypeParameters(typeParameterContainer: FirTypeParameterRefsOwner) =
        generateTypeParameters(typeParameterContainer, describe = true)

    private fun FlowContent.describeVerbose(symbol: FirBasedSymbol<*>) {
        when (symbol) {
            is FirClassLikeSymbol<*> ->
                when (val fir = symbol.fir) {
                    is FirRegularClass -> {
                        declarationStatus(fir.status)
                        classKind(fir.classKind)
                        ws
                        symbolRef(symbol) {
                            +fir.classId.asString()
                        }
                        describeTypeParameters(fir)
                    }
                    is FirTypeAlias -> {
                        keyword("typealias ")
                        symbolRef(symbol) {
                            +symbol.classId.asString()
                        }
                        describeTypeParameters(fir)
                    }
                    else -> +symbol.describe()
                }
            is FirCallableSymbol<*> -> {
                when (val fir = symbol.fir) {
                    is FirSimpleFunction -> {
                        declarationStatus(fir.status)
                        keyword("fun ")
                        describeVerbose(symbol, fir)
                    }
                    is FirConstructor -> {
                        declarationStatus(fir.status)
                        keyword("constructor ")
                        describeVerbose(symbol, fir)
                    }
                    is FirField -> {
                        declarationStatus(fir.status)
                        keyword("field ")
                        describeVerbose(symbol, fir)
                    }
                    is FirProperty -> {
                        declarationStatus(fir.status)
                        if (fir.isVal)
                            keyword("val ")
                        else if (fir.isVar)
                            keyword("var ")
                        describeVerbose(symbol, fir)
                    }
                    is FirAnonymousFunction,
                    is FirErrorFunction,
                    is FirPropertyAccessor,
                    is FirBackingField,
                    is FirEnumEntry,
                    is FirErrorProperty,
                    is FirValueParameter -> {}
                }
            }
            else -> +symbol.describe()
        }
    }

    private fun FlowContent.symbolRef(symbol: FirBasedSymbol<*>?, body: FlowContent.() -> Unit) {
        val (link, classes) = when (symbol) {
            null -> null to setOf()
            is FirClassLikeSymbol<*> -> linkResolver.classLocation(symbol.classId) to setOf("class-fqn")
            else -> linkResolver.nearSymbolLocation(symbol) to setOf("symbol")
        }
        declarationRef(link, classes) {
            if (symbol != null) {
                title = symbol.describe()
            }
            body()
        }
    }

    private fun FlowContent.diagnosticHover(body: FlowContent.() -> Unit) {
        span(classes = "diagnostic-hover") {
            body()
        }
    }

    private fun FlowContent.errorWithDiagnostic(body: FlowContent.() -> Unit) {
        error {
            classes = classes + "diagnostic-hover-container"
            body()
        }
    }

    private fun FlowContent.generate(diagnostic: ConeDiagnostic) {
        when (diagnostic) {
            is ConeInapplicableCandidateError -> {
                describeVerbose(diagnostic.candidate.symbol)
                br
                diagnostic.candidate.errors.forEach { callDiagnostic ->
                    when (callDiagnostic) {
                        is NewConstraintError -> {
                            ident()

                            generate(callDiagnostic.lowerType as ConeKotlinType)

                            ws
                            span(classes = "subtype-error") { +"<:" }
                            ws
                            generate(callDiagnostic.upperType as ConeKotlinType)
                        }
                        else -> {
                            ident()
                            callDiagnostic::class.qualifiedName?.let { +it }
                            Unit
                        }
                    }
                    br
                }
            }
            is ConeAmbiguityError -> {
                +"Ambiguity: "
                br
                for (candidate in diagnostic.candidates) {
                    describeVerbose(candidate.symbol)
                    br
                }
            }
            else -> +diagnostic.reason
        }
    }

    private fun FlowContent.generate(reference: FirReference) {
        when (reference) {
            is FirSuperReference -> keyword("super")
            is FirThisReference -> {
                keyword("this")
                val label = reference.labelName ?: ""
                span("label") {
                    +"@"
                    +label
                }
            }
            is FirErrorNamedReference -> {
                errorWithDiagnostic {
                    simpleName(reference.name)
                    diagnosticHover {
                        generate(reference.diagnostic)
                    }
                }
            }
            is FirSimpleNamedReference -> {
                unresolved {
                    simpleName(reference.name)
                }
            }
            is FirResolvedErrorReference -> {
                errorWithDiagnostic {
                    resolved {
                        symbolRef(reference.resolvedSymbol) {
                            simpleName(reference.name)
                        }
                    }
                    diagnosticHover {
                        generate(reference.diagnostic)
                    }
                }
            }
            is FirResolvedNamedReference -> {
                resolved {
                    symbolRef(reference.resolvedSymbol) {
                        simpleName(reference.name)
                    }
                }
            }
        }
    }

    private fun FlowContent.generateReceiver(access: FirQualifiedAccessExpression) {
        val explicitReceiver = access.explicitReceiver
        if (explicitReceiver != null) {
            generate(explicitReceiver)
            +"."
        }
    }

    private fun FlowContent.generate(functionCall: FirFunctionCall, skipReceiver: Boolean = false) {

        if (!skipReceiver) {
            generateReceiver(functionCall)
        }

        generate(functionCall.calleeReference)
        generateTypeProjections(functionCall.typeArguments)
        +"("
        generateList(functionCall.arguments) {
            generate(it)
        }
        +")"
    }

    private fun FlowContent.generateBinary(first: FirExpression, second: FirExpression, operation: FirOperation) {
        generate(first)
        ws
        unresolved { +operation.operator }
        ws
        generate(second)
    }

    private fun FlowContent.generate(typeOperatorCall: FirTypeOperatorCall) {
        generate(typeOperatorCall.argument)
        ws
        keyword(typeOperatorCall.operation.operator)
        ws
        generate(typeOperatorCall.conversionTypeRef)
    }

    private fun FlowContent.generate(equalityOperatorCall: FirEqualityOperatorCall) {
        generateBinary(equalityOperatorCall.arguments[0], equalityOperatorCall.arguments[1], equalityOperatorCall.operation)
    }

    private fun FlowContent.generate(checkNotNullCall: FirCheckNotNullCall) {
        generate(checkNotNullCall.argument)
        +"!!"
    }

    private fun FlowContent.generate(elvisExpression: FirElvisExpression) {
        generate(elvisExpression.lhs)
        +" ?: "
        generate(elvisExpression.rhs)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun FlowContent.generate(elseIfTrueCondition: FirElseIfTrueCondition) {
        keyword("else")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun FlowContent.generate(whenSubjectExpression: FirWhenSubjectExpression) {
        +"<subj>"
    }

    private fun FlowContent.generateMultiLineExpression(isStatement: Boolean, body: FlowContent.() -> Unit) {
        if (!isStatement) {
            br
            currentIdent++
        }
        body()
        if (!isStatement) {
            currentIdent--
            inl()
        }
    }

    private fun FlowContent.generate(tryExpression: FirTryExpression, isStatement: Boolean) {
        generateMultiLineExpression(isStatement) {
            iline {
                keyword("try ")
                generateBlockIfAny(tryExpression.tryBlock)

                for (catch in tryExpression.catches) {
                    keyword(" catch ")
                    +"("
                    generate(catch.parameter)
                    +") "
                    generateBlockIfAny(catch.block)
                }
                val finallyBlock = tryExpression.finallyBlock
                if (finallyBlock != null) {
                    keyword(" finally ")
                    generateBlockIfAny(finallyBlock)
                }
            }
        }
    }

    private fun FlowContent.generate(whileLoop: FirWhileLoop) {
        iline {
            generateLabel(whileLoop.label)
            keyword("while ")
            +"("
            generate(whileLoop.condition)
            +") "
            generateBlockIfAny(whileLoop.block)
        }
    }

    private fun FlowContent.generate(whenExpression: FirWhenExpression, isStatement: Boolean) {
        generateMultiLineExpression(isStatement) {
            iline {
                keyword("when")
                +" ("
                whenExpression.subjectVariable?.let { generate(it) }
                    ?: whenExpression.subject?.let { generate(it) }
                +") {"
            }
            withIdentLevel {
                for (branch in whenExpression.branches) {
                    inl()
                    generate(branch.condition)
                    +" -> {"
                    if (branch.result.statements.isNotEmpty()) {
                        br
                        withIdentLevel {
                            generateBlockContent(branch.result)
                        }
                        inl()
                    }
                    +"}"
                    br
                }
            }
            iline {
                +"}"
            }
        }
    }

    private fun FlowContent.generate(throwExpression: FirThrowExpression) {
        keyword("throw ")
        generate(throwExpression.exception)
    }

    private fun FlowContent.generate(unitExpression: FirUnitExpression) {
        generate(unitExpression.resolvedType)
    }

    private fun FlowContent.generate(breakExpression: FirBreakExpression) {
        keyword("break")
        span("label") {
            +"@"
            +(breakExpression.target.labelName ?: "")
        }
    }

    private fun FlowContent.generate(continueExpression: FirContinueExpression) {
        keyword("continue")
        span("label") {
            +"@"
            +(continueExpression.target.labelName ?: "")
        }
    }


    private fun FlowContent.generate(initializer: FirAnonymousInitializer) {
        iline {
            keyword("init")
            generateBlockIfAny(initializer.body)
        }
    }

    private fun FlowContent.generate(stringConcatenationCall: FirStringConcatenationCall) {
        generateList(stringConcatenationCall.arguments, separator = " + ") {
            generate(it)
        }
    }

    private fun FlowContent.generateLabel(label: FirLabel?) {
        if (label == null) return
        span("label") {
            +label.name
            +"@"
        }
    }

    private fun FlowContent.generate(getClassCall: FirGetClassCall) {
        generate(getClassCall.argument)
        +"::"
        keyword("class")
    }

    private fun FlowContent.generate(resolvedQualifier: FirResolvedQualifier) {
        resolved {
            val symbol = resolvedQualifier.symbol
            if (symbol != null) {
                symbolRef(symbol) {
                    fqn(resolvedQualifier.classId?.relativeClassName ?: FqName("<???>"))
                }
            } else {
                fqn(resolvedQualifier.packageFqName)
            }
        }
    }

    private fun FlowContent.generate(expression: FirExpression) {
        exprType(expression.resolvedType.toFirResolvedTypeRef()) {
            when (expression) {
                is FirBlock -> generateBlockIfAny(expression)
                is FirGetClassCall -> generate(expression)
                is FirContinueExpression -> generate(expression)
                is FirBreakExpression -> generate(expression)
                is FirAnonymousObjectExpression -> generate(expression.anonymousObject)
                is FirAnonymousFunctionExpression -> generate(expression.anonymousFunction, isStatement = false)
                is FirUnitExpression -> generate(expression)
                is FirStringConcatenationCall -> generate(expression)
                is FirThrowExpression -> generate(expression)
                is FirWhenSubjectExpression -> generate(expression)
                is FirElseIfTrueCondition -> generate(expression)
                is FirWhenExpression -> generate(expression, isStatement = false)
                is FirTryExpression -> generate(expression, isStatement = false)
                is FirLiteralExpression<*> -> generate(expression)
                is FirReturnExpression -> {
                    span("return-label") {
                        symbolRef(expression.target.labeledElement.symbol) {
                            +"^"
                            +(expression.target.labelName ?: "")
                        }
                    }
                    generate(expression.result)
                }
                is FirFunctionCall -> {
                    generate(expression)
                }
                is FirResolvedQualifier -> generate(expression)
                is FirQualifiedAccessExpression -> generate(expression)
                is FirNamedArgumentExpression -> {
                    simpleName(expression.name)
                    +" = "
                    if (expression.isSpread) {
                        +"*"
                    }
                    generate(expression.expression)
                }
                is FirSpreadArgumentExpression -> {
                    +"*"
                    generate(expression.expression)

                }
                is FirLambdaArgumentExpression -> {
                    keyword("lambda")
                    +" = "
                    generate(expression.expression)
                }
                is FirTypeOperatorCall -> generate(expression)
                is FirEqualityOperatorCall -> generate(expression)
                is FirBinaryLogicExpression -> generate(expression)
                is FirCheckNotNullCall -> generate(expression)
                is FirElvisExpression -> generate(expression)
                is FirVarargArgumentsExpression -> generate(expression)
                is FirResolvedReifiedParameterReference -> generate(expression)
                is FirComparisonExpression -> generate(expression)
                is FirSafeCallExpression -> generate(expression)
                is FirCheckedSafeCallSubject -> {
                    +"\$subj\$"
                }
                is FirSmartCastExpression -> generate(expression)
                else -> inlineUnsupported(expression)
            }
        }
    }

    private fun FlowContent.generate(comparisonExpression: FirComparisonExpression) {
        generate(comparisonExpression.left)
        +" ${comparisonExpression.operation.operator} "
        generate(comparisonExpression.right)
    }

    private fun FlowContent.generate(smartCastExpression: FirSmartCastExpression) {
        span(classes = "smart-cast") {
            generate(smartCastExpression.originalExpression)
        }
    }

    private fun FlowContent.generate(safeCallExpression: FirSafeCallExpression) {
        generate(safeCallExpression.receiver)

        +"?."

        val selector = safeCallExpression.selector
        if (selector is FirQualifiedAccessExpression && selector.explicitReceiver == safeCallExpression.checkedSubjectRef.value) {
            return when (selector) {
                is FirFunctionCall -> generate(selector, skipReceiver = true)
                else -> generate(selector, skipReceiver = true)
            }
        }

        +"{ "

        generate(selector)

        +" }"
    }

    private fun FlowContent.generate(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference) {
        val typeParameter = resolvedReifiedParameterReference.symbol.fir
        +typeParameter.name.identifier
    }

    private fun FlowContent.generate(varargArgumentExpression: FirVarargArgumentsExpression) {
        generateList(varargArgumentExpression.arguments, separator = ",") { generate(it) }
    }

    private fun FlowContent.generate(binaryLogicExpression: FirBinaryLogicExpression) {
        generate(binaryLogicExpression.leftOperand)
        +" ${binaryLogicExpression.kind.token} "
        generate(binaryLogicExpression.rightOperand)
    }

    private fun FlowContent.generate(qualifiedAccessExpression: FirQualifiedAccessExpression, skipReceiver: Boolean = false) {
        if (!skipReceiver) {
            generateReceiver(qualifiedAccessExpression)
        }
        generate(qualifiedAccessExpression.calleeReference)
    }

    private fun FlowContent.generateBlockContent(block: FirBlock) {
        for (statement in block.statements) {
            generate(statement)
        }
    }

    private fun FlowContent.symbolAnchor(symbol: FirBasedSymbol<*>, body: FlowContent.() -> Unit) {
        span(classes = "declaration") {
            id = linkResolver.symbolSignature(symbol)
            body()
        }
    }

    private fun FlowContent.generate(valueParameter: FirValueParameter) {
        if (valueParameter.isVararg) {
            keyword("vararg ")
        }
        if (valueParameter.isCrossinline) {
            keyword("crossinline ")
        }
        if (valueParameter.isNoinline) {
            keyword("noinline ")
        }
        symbolAnchor(valueParameter.symbol) { simpleName(valueParameter.name) }
        +": "
        generate(valueParameter.returnTypeRef)
        val defaultValue = valueParameter.defaultValue
        if (defaultValue != null) {
            +" = "
            generate(defaultValue)
        }
    }

    private fun FlowContent.generateBlockIfAny(block: FirBlock?) {
        if (block == null) return
        +" {"
        generateMultiLineExpression(isStatement = false) {
            generateBlockContent(block)
        }
        +"}"
    }

    private fun FlowContent.generate(anonymousObject: FirAnonymousObject, isStatement: Boolean = false) {
        generateMultiLineExpression(isStatement) {
            iline {
                keyword("object ")
                val superTypeRefs = anonymousObject.superTypeRefs
                if (superTypeRefs.isNotEmpty()) {
                    +": "
                    generateList(superTypeRefs) {
                        generate(it)
                    }
                }
                generateDeclarations(anonymousObject.declarations)
            }
        }
    }

    private fun FlowContent.generateDeclarations(declarations: List<FirDeclaration>) {
        if (declarations.isNotEmpty()) {
            +" {"
            br

            withIdentLevel {
                for (declaration in declarations) {
                    generate(declaration)
                    line {}
                }
            }

            inl()
            +"}"
        }
    }

    private fun FlowContent.generate(function: FirSimpleFunction) {
        generateMultiLineExpression(isStatement = true) {
            iline {
                declarationStatus(function.status)
                keyword("fun ")
                generateTypeParameters(function)
                generateReceiver(function)
                symbolAnchor(function.symbol) {
                    simpleName(function.name)
                }
                +"("
                generateList(function.valueParameters) {
                    generate(it)
                }
                +"): "
                generate(function.returnTypeRef)
                generateBlockIfAny(function.body)
            }
        }
    }

    private fun FlowContent.generate(function: FirConstructor) {
        iline {
            declarationStatus(function.status)
            symbolAnchor(function.symbol) {
                keyword("constructor")
            }
            generateTypeParameters(function)
            +"("
            generateList(function.valueParameters) {
                generate(it)
            }
            +"): "
            generate(function.returnTypeRef)

            val delegatedConstructorCall = function.delegatedConstructor
            val body = function.body
            if (delegatedConstructorCall != null || body != null) {
                +" {"
                generateMultiLineExpression(isStatement = false) {
                    if (delegatedConstructorCall != null) {
                        generate(delegatedConstructorCall)
                    }
                    if (body != null) generateBlockContent(body)
                }
                +"}"
            }
        }
    }

    private fun FlowContent.generate(anonymousFunction: FirAnonymousFunction, isStatement: Boolean) {
        generateMultiLineExpression(isStatement) {
            iline {
                generateLabel(anonymousFunction.label)
                keyword("fun ")
                generateReceiver(anonymousFunction.receiverParameter)

                +"("
                generateList(anonymousFunction.valueParameters) {
                    generate(it)
                }
                +"): "
                generate(anonymousFunction.returnTypeRef)
                generateBlockIfAny(anonymousFunction.body)
            }
        }
    }

    private fun FlowContent.generate(declaration: FirDeclaration) {
        when (declaration) {
            is FirAnonymousInitializer -> generate(declaration)
            is FirMemberDeclaration -> generate(declaration)
            else -> unsupported(declaration)
        }
    }

    private fun FlowContent.generate(import: FirImport) {
        fun simpleRender() {
            val fqName = import.importedFqName
            if (fqName != null) {
                fqn(fqName)
                if (import.isAllUnder)
                    +"."
            }
            if (import.isAllUnder) {
                +"*"
            }
        }

        line {
            keyword("import")
            ws
            when (import) {
                is FirResolvedImport -> {
                    val classId = import.resolvedParentClassId
                    if (classId == null) {
                        val importedFqName = import.importedFqName
                        if (importedFqName != null) {
                            fqn(importedFqName)
                        } else {
                            error { +"no fqn" }
                        }
                    } else {
                        +classId.asString()
                    }
                    if (import.isAllUnder) {
                        +".*"
                    }
                }
                else -> {
                    unresolved {
                        simpleRender()
                    }
                }
            }
            val alias = import.aliasName
            if (alias != null) {
                ws
                keyword("as")
                ws
                simpleName(alias)
            }
        }
    }

    private fun FlowContent.generate(enumEntry: FirEnumEntry) {
        iline {
            simpleName(enumEntry.name)
        }
    }

    private fun HTML.generate(file: FirFile) {
        head {
            title { +file.name }
            commonHead()
            with(linkResolver) {
                supplementary()
            }
        }
        body {
            h4 {
                +"Source: "
                val vFile = file.psi?.containingFile?.virtualFile
                if (vFile != null) {
                    a(href = vFile.url, classes = "container-ref") {
                        +vFile.path
                    }
                } else {
                    +"No source"
                }
            }
            h2 {
                +file.name
            }
            pre {
                packageName(file.packageFqName)
                line {}
                for (import in file.imports) {
                    generate(import)
                }
                line {}
                for (declaration in file.declarations) {
                    generate(declaration)
                    line {}
                }
            }
        }
    }

}
