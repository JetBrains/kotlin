/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirTypePlaceholderProjection
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker
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
}

private class ModuleInfo(val name: String, outputRoot: File) {
    val packages = mutableMapOf<FqName, PackageInfo>()
    val moduleRoot = outputRoot.resolve(name).also {
        it.mkdirs()
    }
    val errors: Map<FqName, Int> by lazy {
        packages.mapValues { (_, packageInfo) -> packageInfo.errors.values.sum() }.withDefault { 0 }
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
                            addErrors(moduleInfo.errors.getValue(packageInfo.fqName))
                        }
                    }
                }
            }
        }
    }

    fun LI.addErrors(errors: Int) {
        if (errors > 0) {
            span(classes = "error-counter") { +(errors.toString()) }
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
                            addErrors(packageInfo.errors.getValue(file))
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
                            addErrors(module.errors.values.sum())
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
        val dumper = HtmlFirDump(LinkResolver(dumpOutput), file.session)
        val builder = StringBuilder()
        dumper.generate(file, builder)

        dumpOutput.writeText(builder.toString())
        packageForFile(file).apply {
            errors[file.name] = dumper.errors
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

                override fun visitSealedClass(sealedClass: FirSealedClass) {
                    visitRegularClass(sealedClass)
                }

                fun indexDeclaration(symbolOwner: FirSymbolOwner<*>) {
                    symbols[symbolOwner.symbol] = location
                    symbolIds[symbolOwner.symbol] = symbolCounter++
                }

                override fun <F : FirVariable<F>> visitVariable(variable: FirVariable<F>) {
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

    fun generate(element: FirFile, builder: StringBuilder): Int {
        errors = 0
        builder.appendHTML().html {
            generate(element)
        }
        return errors
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
        keyword(modality.name.toLowerCase())
    }

    private fun FlowContent.visibility(visibility: Visibility) {
        if (visibility == Visibilities.UNKNOWN)
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


    private fun FlowContent.generate(klass: FirRegularClass) {
        inl()

        declarationStatus(klass.status)


        when (klass.classKind) {
            ClassKind.CLASS -> keyword("class")
            ClassKind.INTERFACE -> keyword("interface")
            ClassKind.ENUM_CLASS -> keyword("enum class")
            ClassKind.ENUM_ENTRY -> Unit // ?
            ClassKind.ANNOTATION_CLASS -> keyword("annotation class")
            ClassKind.OBJECT -> keyword("object")
        }
        ws
        anchoredName(klass.name, klass.classId.asString())
        if (klass.superTypeRefs.isNotEmpty()) {
            +": "
            generateList(klass.superTypeRefs) {
                generate(it)
            }
        }

        generateDeclarations(klass.declarations)
        br

    }

    private fun FlowContent.generate(flexibleType: ConeFlexibleType) {
        if (flexibleType.lowerBound.nullability == ConeNullability.NOT_NULL &&
            flexibleType.upperBound.nullability == ConeNullability.NULLABLE &&
            AbstractStrictEqualityTypeChecker.strictEqualTypes(
                session.typeContext,
                flexibleType.lowerBound,
                flexibleType.upperBound.withNullability(ConeNullability.NOT_NULL)
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

    private fun FlowContent.generate(type: ConeClassType) {
        resolved {
            symbolRef(type.lookupTag.toSymbol(session)) {
                fqn(type.lookupTag.classId.relativeClassName)
            }
        }
    }

    private fun FlowContent.generate(variableAssignment: FirVariableAssignment) {
        generateReceiver(variableAssignment)
        generate(variableAssignment.lValue)
        +" = "
        generate(variableAssignment.rValue)
    }

    private fun FlowContent.generate(projection: ConeKotlinTypeProjection) {
        when (projection) {
            is ConeStarProjection -> +"*"
            is ConeTypedProjection -> {
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

    private fun FlowContent.generate(expression: FirConstExpression<*>) {
        val value = expression.value
        if (value == null && expression.kind != IrConstKind.Null) {
            return error {
                +"null value"
            }
        }

        when (expression.kind) {
            IrConstKind.Null -> keyword("null")
            IrConstKind.Boolean -> keyword(value.toString())
            IrConstKind.String, IrConstKind.Char ->
                stringLiteral(value)
            IrConstKind.Byte -> {
                +value.toString()
                keyword("B")
            }
            IrConstKind.Short -> {
                +value.toString()
                keyword("S")
            }
            IrConstKind.Int -> {
                +value.toString()
                keyword("I")
            }
            IrConstKind.Long -> {
                +value.toString()
                keyword("L")
            }
            IrConstKind.Float -> {
                +value.toString()
                keyword("F")
            }
            IrConstKind.Double -> {
                +value.toString()
                keyword("D")
            }
        }

    }

    private fun FlowContent.generate(type: ConeKotlinType) {
        when (type) {
            is ConeClassErrorType -> error { +type.reason }
            is ConeClassType -> generate(type)
            is ConeAbbreviatedType -> resolved {
                symbolRef(type.abbreviationLookupTag.toSymbol(session)) {
                    simpleName(type.abbreviationLookupTag.name)
                }
                +" = "
                generate(type.directExpansionType(session) ?: ConeKotlinErrorType("No expansion for type-alias"))
            }
            is ConeTypeParameterType -> resolved {
                symbolRef(type.lookupTag.toSymbol()) {
                    simpleName(type.lookupTag.name)
                }
            }
            is ConeTypeVariableType -> resolved { +type.lookupTag.name.asString() }
            is ConeFlexibleType -> resolved { generate(type) }
            is ConeCapturedType -> inlineUnsupported(type)
            is ConeDefinitelyNotNullType -> inlineUnsupported(type)
            is ConeIntersectionType -> resolved { generate(type) }
        }
        if (type.typeArguments.isNotEmpty()) {
            +"<"
            generateList(type.typeArguments.toList()) {
                generate(it)
            }
            +">"
        }
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
            is FirTypePlaceholderProjection -> +"_"
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
            is FirImplicitTypeRef -> unresolved { keyword("<implicit>") }
            is FirUserTypeRef -> unresolved {
                generateList(typeRef.qualifier, separator = ".") {
                    simpleName(it.name)
                    generateTypeProjections(it.typeArguments)
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
            is FirRegularClass -> generate(memberDeclaration)
            is FirSimpleFunction -> generate(memberDeclaration)
            is FirProperty -> if (memberDeclaration.isLocal) generate(memberDeclaration as FirVariable<*>) else generate(memberDeclaration)
            is FirConstructor -> generate(memberDeclaration)
            else -> unsupported(memberDeclaration)
        }
    }

    private fun FlowContent.generateTypeParameters(typeParameterContainer: FirTypeParametersOwner) {
        if (typeParameterContainer.typeParameters.isEmpty()) return
        +"<"
        generateList(typeParameterContainer.typeParameters) {
            generate(it.variance)
            if (it.isReified) {
                keyword("reified ")
            }
            symbolAnchor(it.symbol) {
                simpleName(it.name)
            }
            if (it.bounds.isNotEmpty()) {
                +": "
                generateList(it.bounds) { bound ->
                    generate(bound)
                }
            }
        }
        +"> "
    }

    private fun FlowContent.generateReceiver(declaration: FirCallableDeclaration<*>) {
        generateReceiver(declaration.receiverTypeRef)
    }

    private fun FlowContent.generateReceiver(receiverTypeRef: FirTypeRef?) {
        receiverTypeRef ?: return
        generate(receiverTypeRef)
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
            is FirVariable<*> -> iline { generate(statement) }
            is FirVariableAssignment -> iline { generate(statement) }
            else -> unsupported(statement)
        }
    }

    private fun FlowContent.generate(variable: FirVariable<*>) {
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
            else -> ""
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
                error {
                    title = reference.diagnostic.reason
                    simpleName(reference.name)
                }
            }
            is FirSimpleNamedReference -> {
                unresolved {
                    simpleName(reference.name)
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

    private fun FlowContent.generateReceiver(access: FirQualifiedAccess) {
        val explicitReceiver = access.explicitReceiver
        if (explicitReceiver != null) {
            generate(explicitReceiver)
            if (access.safe) {
                +"?."
            } else {
                +"."
            }
        }
    }

    private fun FlowContent.generate(functionCall: FirFunctionCall) {
        generateReceiver(functionCall)

        generate(functionCall.calleeReference)
        generateTypeProjections(functionCall.typeArguments)
        +"("
        generateList(functionCall.arguments) {
            generate(it)
        }
        +")"
    }

    private fun FlowContent.generateBinary(expression: FirOperatorCall) {
        val (first, second) = expression.arguments
        generate(first)
        ws
        unresolved { +expression.operation.operator }
        ws
        generate(second)
    }

    private fun FlowContent.generateUnary(expression: FirOperatorCall) {
        val (first) = expression.arguments
        unresolved { +expression.operation.operator }
        ws
        generate(first)
    }

    private fun FlowContent.generate(expression: FirOperatorCall) {
        when (expression.arguments.size) {
            1 -> generateUnary(expression)
            2 -> generateBinary(expression)
            else -> inlineUnsupported(expression)
        }
    }

    private fun FlowContent.generate(typeOperatorCall: FirTypeOperatorCall) {
        val (expression) = typeOperatorCall.arguments
        generate(expression)
        ws
        keyword(typeOperatorCall.operation.operator)
        ws
        generate(typeOperatorCall.conversionTypeRef)
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
        generate(unitExpression.typeRef)
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
            val symbolProvider = session.firSymbolProvider
            val classId = resolvedQualifier.classId
            if (classId != null) {
                symbolRef(symbolProvider.getClassLikeSymbolByFqName(classId)) {
                    fqn(classId.relativeClassName)
                }
            } else {
                fqn(resolvedQualifier.packageFqName)
            }
        }
    }

    private fun FlowContent.generate(expression: FirExpression) {
        exprType(expression.typeRef) {
            when (expression) {
                is FirGetClassCall -> generate(expression)
                is FirContinueExpression -> generate(expression)
                is FirBreakExpression -> generate(expression)
                is FirAnonymousObject -> generate(expression)
                is FirUnitExpression -> generate(expression)
                is FirStringConcatenationCall -> generate(expression)
                is FirAnonymousFunction -> generate(expression, isStatement = false)
                is FirThrowExpression -> generate(expression)
                is FirWhenSubjectExpression -> generate(expression)
                is FirElseIfTrueCondition -> generate(expression)
                is FirWhenExpression -> generate(expression, isStatement = false)
                is FirTryExpression -> generate(expression, isStatement = false)
                is FirConstExpression<*> -> generate(expression)
                is FirReturnExpression -> {
                    span("return-label") {
                        symbolRef((expression.target.labeledElement as? FirSymbolOwner<*>)?.symbol) {
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
                is FirOperatorCall -> generate(expression)
                is FirBinaryLogicExpression -> generate(expression)
                else -> inlineUnsupported(expression)
            }
        }
    }

    private fun FlowContent.generate(binaryLogicExpression: FirBinaryLogicExpression) {
        generate(binaryLogicExpression.leftOperand)
        +" ${binaryLogicExpression.kind.token} "
        generate(binaryLogicExpression.rightOperand)
    }

    private fun FlowContent.generate(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        generateReceiver(qualifiedAccessExpression)
        generate(qualifiedAccessExpression.calleeReference)
    }

    private fun FlowContent.generateBlockContent(block: FirBlock) {
        for (statement in block.statements) {
            generate(statement)
        }
    }

    private fun FlowContent.symbolAnchor(symbol: AbstractFirBasedSymbol<*>, body: FlowContent.() -> Unit) {
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
            generateBlockIfAny(function.body)
        }
    }

    private fun FlowContent.generate(anonymousFunction: FirAnonymousFunction, isStatement: Boolean) {
        generateMultiLineExpression(isStatement) {
            iline {
                generateLabel(anonymousFunction.label)
                keyword("fun ")
                generateReceiver(anonymousFunction.receiverTypeRef)

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
                    val classId = import.resolvedClassId
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
