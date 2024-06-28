/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.CustomKotlinLikeDumpStrategy.Modifiers
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer

/**
 * Conventions:
 * * For unsupported cases (node, type) it prints a block comment which starts with "/* ERROR:"               (*/<- hack for parser)
 * * Conventions for some operators:
 *     * IMPLICIT_CAST -- expr /*as Type */
 *     * IMPLICIT_NOTNULL -- expr /*!! Type */
 *     * IMPLICIT_COERCION_TO_UNIT -- expr /*~> Unit */
 *     * IMPLICIT_INTEGER_COERCION -- expr /*~> IntType */
 *     * SAM_CONVERSION -- expr /*-> SamType */
 *     * IMPLICIT_DYNAMIC_CAST -- expr /*~> dynamic */
 *     * REINTERPRET_CAST -- expr /*=> Type */
 */
fun IrElement.dumpKotlinLike(options: KotlinLikeDumpOptions = KotlinLikeDumpOptions()): String =
    dumpKotlinLike(this, KotlinLikeDumper::printElement, options)

fun IrType.dumpKotlinLike(): String =
    dumpKotlinLike(this, KotlinLikeDumper::printType)

fun IrTypeArgument.dumpKotlinLike(): String =
    dumpKotlinLike(this, KotlinLikeDumper::printTypeArgument)

private inline fun <T> dumpKotlinLike(
    target: T,
    print: KotlinLikeDumper.(T) -> Unit,
    options: KotlinLikeDumpOptions = KotlinLikeDumpOptions()
): String {
    val sb = StringBuilder()
    KotlinLikeDumper(Printer(sb, 1, "  "), options).print(target)
    return sb.toString()
}

data class KotlinLikeDumpOptions(
    val customDumpStrategy: CustomKotlinLikeDumpStrategy = CustomKotlinLikeDumpStrategy.Default,
    val printRegionsPerFile: Boolean = false,
    val printFileName: Boolean = true,
    val printFilePath: Boolean = true,
    // TODO support
    val useNamedArguments: Boolean = false,
    // TODO support
    val labelPrintingStrategy: LabelPrintingStrategy = LabelPrintingStrategy.NEVER,
    val printFakeOverridesStrategy: FakeOverridesStrategy = FakeOverridesStrategy.ALL,
    val bodyPrintingStrategy: BodyPrintingStrategy = BodyPrintingStrategy.PRINT_BODIES,
    val printElseAsTrue: Boolean = false,
    val printUnitReturnType: Boolean = false,
    val stableOrder: Boolean = false,
    val normalizeNames: Boolean = false,
    /*
    TODO add more options:
     always print visibility?
     omit local visibility?
     always print modality
     print special names as is, and other strategies?
     print body for default accessors
     print get/set for default accessors
     print type of expressions
     ability to omit all or part of types for local variables
     omit IrSyntheticBody? or even whole declaration
     */
)

enum class LabelPrintingStrategy {
    NEVER,
    ALWAYS,
    SMART // WHEN_REQUIRED
}

enum class FakeOverridesStrategy {
    ALL,
    ALL_EXCEPT_ANY,
    NONE
}

enum class BodyPrintingStrategy {
    NO_BODIES,
    PRINT_ONLY_LOCAL_CLASSES_AND_FUNCTIONS,
    PRINT_BODIES,
}

/**
 * An interface for customizing the Kotlin-like dump.
 * It allows to e.g. skip certain declarations or annotations from the dump, or print arbitrary text before and after each IR element.
 */
interface CustomKotlinLikeDumpStrategy {

    fun shouldPrintAnnotation(annotation: IrConstructorCall, container: IrAnnotationContainer): Boolean = true

    fun willPrintElement(element: IrElement, container: IrDeclaration?, printer: Printer): Boolean = true

    fun didPrintElement(element: IrElement, container: IrDeclaration?, printer: Printer) {}

    fun transformModifiersForDeclaration(declaration: IrDeclaration, modifiers: Modifiers): Modifiers = modifiers

    data class Modifiers(
        val visibility: DescriptorVisibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
        val isExpect: Boolean = false,
        val modality: Modality? = null,
        val isExternal: Boolean = false,
        val isOverride: Boolean = false,
        val isFakeOverride: Boolean = false,
        val isLateinit: Boolean = false,
        val isTailrec: Boolean = false,
        val isSuspend: Boolean = false,
        val isInner: Boolean = false,
        val isInline: Boolean = false,
        val isValue: Boolean = false,
        val isData: Boolean = false,
        val isCompanion: Boolean = false,
        val isFunInterface: Boolean = false,
        val classKind: ClassKind? = null,
        val isInfix: Boolean = false,
        val isOperator: Boolean = false,
        val isVararg: Boolean = false,
        val isCrossinline: Boolean = false,
        val isNoinline: Boolean = false,
        val isHidden: Boolean = false,
        val isAssignable: Boolean = false,
    )

    object Default : CustomKotlinLikeDumpStrategy
}

// TODO_ conventions:
// TODO support -- for unsupported nodes
// TODO no test -- for the cases with no test(s)
// it's not valid kotlin -- for the cases when used some syntax which is invalid in Kotlin, maybe they are worth to reconsider

/* TODO:
    * origin : class, function, property, ...
        * option?
    * don't print members of kotlin.Any in interfaces? // or just print something like  /* Any members */
        * option?
    * use lambda syntax for FunctionN, including extension lambdas (@ExtensionFunctionType)
        * option?
    * use FQNs?
        * option?
    * don't print coercion to Unit on top level? and inside blocks?
        * option?
    * special render for call site of accessors?
        * option?
    * FlexibleNullability is it about `T!`?
        * option?
    * unique ids for symbols, or SignatureID?
        * option?
    * wrap/escape invalid identifiers with "`", like "$$delegate"
 */

private class KotlinLikeDumper(val p: Printer, val options: KotlinLikeDumpOptions) : IrElementVisitor<Unit, IrDeclaration?> {
    private val variableNameData = VariableNameData(options.normalizeNames)

    private val IrSymbol.safeName
        get() = if (!isBound) {
            "/* ERROR: unbound symbol $signature */"
        } else {
            when (val owner = owner) {
                is IrVariable -> owner.normalizedName(variableNameData)
                is IrDeclarationWithName -> owner.name.toString()
                else -> "/* ERROR: unnamed symbol $signature */"
            }
        }

    private val IrFunctionSymbol.safeValueParameters
        get() = if (!isBound) {
            emptyList()
        } else {
            owner.valueParameters
        }

    private val IrSymbol.safeParentClassName
        get() = if (!isBound) {
            "/* ERROR: unbound symbol $signature */"
        } else {
            (owner as? IrDeclaration)?.parentClassOrNull?.name?.toString() ?: "/* ERROR: unexpected parent for $safeName */"
        }

    private val IrSymbol.safeParentClassOrNull
        get() = if (!isBound) {
            null
        } else {
            (owner as? IrDeclaration)?.parentClassOrNull
        }


    fun printElement(element: IrElement) {
        element.accept(this, null)
    }

    fun printType(type: IrType) {
        type.printTypeWithNoIndent()
    }

    fun printTypeArgument(typeArg: IrTypeArgument) {
        typeArg.printTypeArgumentWithNoIndent()
    }

    @JvmName("orderedDeclarations") // Prevent JVM signature clash
    private fun List<IrDeclaration>.ordered() = if (options.stableOrder) stableOrdered() else this

    @JvmName("orderedTypes") // Prevent JVM signature clash
    private fun List<IrType>.ordered(): List<IrType> {
        if (!options.stableOrder) return this

        fun isNonInterfaceType(type: IrType) = type.classifierOrNull?.let {
            it !is IrClassSymbol || !it.owner.isInterface
        } ?: true

        val (classTypes, interfaceTypes) = partition(::isNonInterfaceType)

        return classTypes.sortedBy(IrType::render) + interfaceTypes.sortedBy(IrType::render)
    }

    private inline fun wrap(element: IrElement, container: IrDeclaration?, block: () -> Unit) {
        if (!options.customDumpStrategy.willPrintElement(element, container, p)) return
        try {
            block()
        } finally {
            options.customDumpStrategy.didPrintElement(element, container, p)
        }
    }

    override fun visitElement(element: IrElement, data: IrDeclaration?) {
        val e = "/* ERROR: unsupported element type: " + element.javaClass.simpleName + " */"
        if (element is IrExpression) {
            // TODO move text to the message?
            // TODO better process expressions and statements
            p.printlnWithNoIndent("error(\"\") $e")
        } else {
            p.println(e)
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: IrDeclaration?) = wrap(declaration, data) {
        p.println("// MODULE: ${declaration.name.asString()}")
        declaration.acceptChildren(this, null)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFile(declaration: IrFile, data: IrDeclaration?) = wrap(declaration, data) {
        if (options.printRegionsPerFile) p.println("//region block: ${declaration.name}")

        if (options.printFileName) p.println("// FILE: ${declaration.name}")
        if (options.printFilePath) p.println("// path: ${declaration.path}")
        declaration.printlnAnnotations("file")
        val packageFqName = declaration.packageFqName
        if (!packageFqName.isRoot) {
            p.println("package ${packageFqName.asString()}")
        }
        if (!p.isEmpty) p.printlnWithNoIndent()

        declaration.declarations.ordered().forEach { it.accept(this, null) }

        if (options.printRegionsPerFile) p.println("//endregion")
    }

    override fun visitClass(declaration: IrClass, data: IrDeclaration?) = wrap(declaration, data) {
        // TODO omit super class for enums, annotations?
        // TODO omit Companion name for companion objects?
        // TODO do we need to print info about `thisReceiver`?
        // TODO special support for objects?

        declaration.printlnAnnotations()
        p.printIndent()

        declaration.run {
            printModifiersWithNoIndent(
                this,
                Modifiers(
                    visibility = visibility,
                    isExpect = isExpect,
                    modality = modality,
                    isExternal = isExternal,
                    isInner = isInner,
                    isValue = isValue,
                    isData = isData,
                    isCompanion = isCompanion,
                    isFunInterface = isFun,
                    classKind = kind,
                ),
            )
        }

        p.printWithNoIndent(declaration.name.asString())

        declaration.printTypeParametersWithNoIndent()
        // TODO no test
        if (declaration.superTypes.isNotEmpty()) {
            var first = true
            for (type in declaration.superTypes.ordered()) {
                if (type.isAny()) continue

                if (!first) {
                    p.printWithNoIndent(", ")
                } else {
                    p.printWithNoIndent(" : ")
                    first = false
                }

                type.printTypeWithNoIndent()
            }

        }

        declaration.printWhereClauseIfNeededWithNoIndent()

        p.printlnWithNoIndent(" {")
        p.pushIndent()

        declaration.declarations.ordered().forEach { it.accept(this, declaration) }

        p.popIndent()
        p.println("}")
        p.printlnWithNoIndent()
    }

    /*
    https://kotlinlang.org/docs/reference/coding-conventions.html#modifiers
    Modifiers order:
    public / protected / private / internal
    expect / actual
    final / open / abstract / sealed / const
    external
    override
    lateinit
    tailrec
    vararg
    suspend
    inner
    enum / annotation / fun // as a modifier in `fun interface`
    companion
    inline
    value
    infix
    operator
    data
     */
    private fun printModifiersWithNoIndent(declaration: IrDeclaration, modifiers: Modifiers) =
        options.customDumpStrategy.transformModifiersForDeclaration(declaration, modifiers).run {
            val isInterfaceMember = declaration is IrOverridableMember && (declaration.parent as? IrClass)?.isInterface == true
            printVisibility(visibility)
            p(isExpect, "expect") // TODO actual?
            val defaultModality = when {
                isInterfaceMember || (isOverride || isFakeOverride) && modality == Modality.OPEN ->
                    Modality.OPEN
                classKind == ClassKind.INTERFACE ->
                    Modality.ABSTRACT
                else ->
                    Modality.FINAL
            }
            p(modality, defaultModality) { name.lowercase() }
            p(isExternal, "external")
            p(isFakeOverride, customModifier("fake"))
            p(isOverride, "override")
            p(isLateinit, "lateinit")
            p(isTailrec, "tailrec")
            printParameterModifiersWithNoIndent(
                isVararg,
                isCrossinline,
                isNoinline,
                isHidden,
                isAssignable,
            )
            p(isSuspend, "suspend")
            p(isInner, "inner")
            p(isInline, "inline")
            p(isValue, "value")
            p(isData, "data")
            p(isCompanion, "companion")
            p(isFunInterface, "fun")
            p(classKind) { name.lowercase().replace('_', ' ') + if (this == ClassKind.ENUM_ENTRY) " class" else "" }
            p(isInfix, "infix")
            p(isOperator, "operator")
        }

    private fun printVisibility(visibility: DescriptorVisibility) {
        // TODO don't print visibility if it's not changed in override?
        p(visibility, DescriptorVisibilities.DEFAULT_VISIBILITY) { name }
    }

    private fun printParameterModifiersWithNoIndent(
        isVararg: Boolean,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        isHidden: Boolean,
        isAssignable: Boolean,
    ) {
        p(isVararg, "vararg")
        p(isCrossinline, "crossinline")
        p(isNoinline, "noinline")
        p(isHidden, customModifier("hidden"))
        p(isAssignable, customModifier("var"))
    }

    private fun IrTypeParametersContainer.printTypeParametersWithNoIndent(postfix: String = "") {
        if (typeParameters.isEmpty()) return

        p.printWithNoIndent("<")
        typeParameters.forEachIndexed { i, typeParam ->
            p(i > 0, ",")

            typeParam.printATypeParameterWithNoIndent()
        }
        p.printWithNoIndent(">")
        p.printWithNoIndent(postfix)
    }

    private fun IrTypeParameter.printATypeParameterWithNoIndent() {
        variance.printVarianceWithNoIndent()
        p(isReified, "reified")

        printAnnotationsWithNoIndent()

        p.printWithNoIndent(name.asString())

        if (superTypes.size == 1) {
            p.printWithNoIndent(" : ")
            superTypes.single().printTypeWithNoIndent()
        }
    }

    private fun Variance.printVarianceWithNoIndent() {
        p(this, Variance.INVARIANT) { label }
    }

    private fun filterAnnotations(annotations: List<IrConstructorCall>, container: IrAnnotationContainer): List<IrConstructorCall> =
        annotations.filter { options.customDumpStrategy.shouldPrintAnnotation(it, container) }

    private fun IrAnnotationContainer.printAnnotationsWithNoIndent() {
        filterAnnotations(annotations, this).forEach {
            it.printAnAnnotationWithNoIndent()
            p.printWithNoIndent(" ")
        }
    }

    private fun IrAnnotationContainer.printlnAnnotations(prefix: String = "") {
        filterAnnotations(annotations, this).forEach {
            p.printIndent()
            it.printAnAnnotationWithNoIndent(prefix)
            p.printlnWithNoIndent()
        }
    }

    private fun IrConstructorCall.printAnAnnotationWithNoIndent(prefix: String = "") {
        p.printWithNoIndent("@" + (if (prefix.isEmpty()) "" else "$prefix:"))
        visitConstructorCall(this, null)
    }

    private fun IrTypeParametersContainer.printWhereClauseIfNeededWithNoIndent() {
        if (typeParameters.none { it.superTypes.size > 1 }) return

        p.printWithNoIndent(" where ")

        var first = true
        typeParameters.forEach {
            if (it.superTypes.size > 1) {
                // TODO no test with more than one generic parameter with more supertypes
                first = it.printWhereClauseTypesWithNoIndent(first)
            }
        }
    }

    private fun IrTypeParameter.printWhereClauseTypesWithNoIndent(first: Boolean): Boolean {
        var myFirst = first
        superTypes.ordered().forEach { type ->
            if (!myFirst) {
                p.printWithNoIndent(", ")
            } else {
                myFirst = false
            }

            p.printWithNoIndent(name.asString())
            p.printWithNoIndent(" : ")
            type.printTypeWithNoIndent()
        }

        return myFirst
    }

    private fun IrType.printTypeWithNoIndent() {
        // TODO don't print `Any?` as upper bound?
        printAnnotationsWithNoIndent()
        when (this) {
            is IrSimpleType -> {
                // TODO abbreviation

                val dnn = classifier is IrTypeParameterSymbol && nullability == SimpleTypeNullability.DEFINITELY_NOT_NULL
                if (dnn) {
                    p.printWithNoIndent("(")
                }

                p.printWithNoIndent(classifier.safeName)

                if (arguments.isNotEmpty()) {
                    p.printWithNoIndent("<")
                    arguments.forEachIndexed { i, typeArg ->
                        p(i > 0, ",")

                        typeArg.printTypeArgumentWithNoIndent()
                    }
                    p.printWithNoIndent(">")
                }

                if (dnn) {
                    p.printWithNoIndent(" & Any)")
                }

                if (isMarkedNullable()) p.printWithNoIndent("?")
            }
            is IrDynamicType ->
                p.printWithNoIndent("dynamic")
            is IrErrorType ->
                p.printWithNoIndent("ErrorType")
            else ->
                p.printWithNoIndent("??? /* ERROR: unknown type: ${this.javaClass.simpleName} */")
        }
    }

    private fun IrTypeArgument.printTypeArgumentWithNoIndent() {
        when (this) {
            is IrStarProjection ->
                p.printWithNoIndent("*")
            is IrTypeProjection -> {
                variance.printVarianceWithNoIndent()
                type.printTypeWithNoIndent()
            }
        }
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: IrDeclaration?) = wrap(declaration, data) {
        declaration.printlnAnnotations()
        p.printIndent()

        printVisibility(declaration.visibility)
        p(declaration.isActual, "actual")

        p.printWithNoIndent("typealias ")
        p.printWithNoIndent(declaration.name.asString())
        declaration.printTypeParametersWithNoIndent()
        p.printWithNoIndent(" = ")
        declaration.expandedType.printTypeWithNoIndent()

        p.printlnWithNoIndent()
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: IrDeclaration?) = wrap(declaration, data) {
        // TODO better rendering for enum entries

        declaration.correspondingClass?.let { p.println() }

        declaration.printlnAnnotations()
        p.printIndent()
        p.printWithNoIndent(declaration.name)
        declaration.initializerExpression?.let {
            if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
                // it's not valid kotlin
                p.printWithNoIndent(" = ")
            }
            it.accept(this, declaration)
        }
        p.println()

        declaration.correspondingClass?.accept(this, declaration)

        p.println()
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: IrDeclaration?) = wrap(declaration, data) {
        // TODO no tests, looks like IrAnonymousInitializer has annotations accidentally.
        declaration.printlnAnnotations()
        p.printIndent()

        // TODO no tests, looks like there are no irText tests for isStatic flag
        p(declaration.isStatic, customModifier("static"))
        p.printWithNoIndent("init")
        if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
            p.printWithNoIndent(" ")
        }
        declaration.body.accept(this, declaration)

        p.printlnWithNoIndent()
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: IrDeclaration?) {
        declaration.printSimpleFunction(
            data,
            "fun ",
            declaration.name.asString(),
            printTypeParametersAndExtensionReceiver = true,
            printSignatureAndBody = true,
            printExtraTrailingNewLine = true,
        )
    }

    override fun visitConstructor(declaration: IrConstructor, data: IrDeclaration?) = wrap(declaration, data) {
        // TODO name?
        // TODO is it worth to merge code for IrConstructor and IrSimpleFunction?
        // TODO dispatchReceiverParameter -- outer `this` for inner classes
        // TODO return type?

        declaration.printlnAnnotations()
        p.printIndent()

        declaration.run {
            printModifiersWithNoIndent(
                this,
                Modifiers(
                    visibility = visibility,
                    isExpect = isExpect,
                    isExternal = isExternal,
                    isInline = isInline,
                ),
            )
        }

        p.printWithNoIndent("constructor")
        declaration.printTypeParametersWithNoIndent()
        declaration.printValueParametersWithNoIndent()
        declaration.printWhereClauseIfNeededWithNoIndent()
        if (declaration.isPrimary) {
            p.printWithNoIndent(" ", customModifier("primary"))
        }
        declaration.body?.let {
            if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
                p.printWithNoIndent(" ")
            }
            it.accept(this, declaration)
        }
        p.printlnWithNoIndent()
        p.printlnWithNoIndent()
    }

    private fun IrSimpleFunction.printSimpleFunction(
        data: IrDeclaration?,
        keyword: String,
        name: String,
        printTypeParametersAndExtensionReceiver: Boolean,
        printSignatureAndBody: Boolean,
        printExtraTrailingNewLine: Boolean
    ) {
        /* TODO
            correspondingProperty
            overridden
            dispatchReceiverParameter
         */

        if (options.printFakeOverridesStrategy == FakeOverridesStrategy.NONE && isFakeOverride ||
            options.printFakeOverridesStrategy == FakeOverridesStrategy.ALL_EXCEPT_ANY && isFakeOverriddenFromAny()
        ) {
            return
        }

        wrap(this, data) {
            printlnAnnotations()
            p.print("")

            printModifiersWithNoIndent(
                this,
                Modifiers(
                    visibility = visibility,
                    isExpect = isExpect,
                    modality = modality,
                    isExternal = isExternal,
                    isOverride = overriddenSymbols.isNotEmpty(),
                    isFakeOverride = isFakeOverride,
                    isLateinit = isTailrec,
                    isSuspend = isSuspend,
                    isInline = isInline,
                    isInfix = isInfix,
                    isOperator = isOperator,
                ),
            )

            p.printWithNoIndent(keyword)

            if (printTypeParametersAndExtensionReceiver) printTypeParametersWithNoIndent(postfix = " ")

            if (printTypeParametersAndExtensionReceiver) {
                extensionReceiverParameter?.printExtensionReceiverParameter()
            }

            p.printWithNoIndent(name)

            if (printSignatureAndBody) {
                printValueParametersWithNoIndent()

                if (options.printUnitReturnType || !returnType.isUnit()) {
                    p.printWithNoIndent(": ")
                    returnType.printTypeWithNoIndent()
                }
                printWhereClauseIfNeededWithNoIndent()

                body?.let {
                    if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
                        p.printWithNoIndent(" ")
                    }
                    it.accept(this@KotlinLikeDumper, null)
                }

            }

            if (!printSignatureAndBody || body == null || options.bodyPrintingStrategy != BodyPrintingStrategy.PRINT_BODIES) {
                p.printlnWithNoIndent()
            }

            if (printExtraTrailingNewLine)
                p.printlnWithNoIndent()
        }
    }

    private fun IrValueParameter.printExtensionReceiverParameter() {
        type.printTypeWithNoIndent()
        p.printWithNoIndent(".")
    }

    private fun IrFunction.printValueParametersWithNoIndent() {
        p.printWithNoIndent("(")
        valueParameters.forEachIndexed { i, param ->
            p(i > 0, ",")

            param.printAValueParameterWithNoIndent(this)
        }
        p.printWithNoIndent(")")
    }

    private fun IrValueParameter.printAValueParameterWithNoIndent(data: IrDeclaration?) {
        printAnnotationsWithNoIndent()

        printParameterModifiersWithNoIndent(
            isVararg = varargElementType != null,
            isCrossinline,
            isNoinline,
            // TODO no test
            isHidden,
            // TODO no test
            isAssignable
        )

        p.printWithNoIndent(name.asString())
        p.printWithNoIndent(": ")
        (varargElementType ?: type).printTypeWithNoIndent()
        // TODO print it.type too for varargs?

        defaultValue?.let { v ->
            if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
                p.printWithNoIndent(" = ")
            }
            v.accept(this@KotlinLikeDumper, data)
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: IrDeclaration?) = wrap(declaration, data) {
        declaration.printATypeParameterWithNoIndent()
        if (declaration.superTypes.size > 1) declaration.printWhereClauseTypesWithNoIndent(true)
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: IrDeclaration?) = wrap(declaration, data) {
        // TODO index?
        declaration.printAValueParameterWithNoIndent(data)
    }

    override fun visitProperty(declaration: IrProperty, data: IrDeclaration?) = wrap(declaration, data) {
        if (options.printFakeOverridesStrategy == FakeOverridesStrategy.NONE && declaration.isFakeOverride) return

        declaration.printlnAnnotations()
        p.printIndent()

        // TODO better rendering for modifiers on property and accessors
        //  modifiers that could be different between accessors and property have a comment
        declaration.run {
            printModifiersWithNoIndent(
                this,
                Modifiers(
                    // accessors by default have same visibility, but the can define own value
                    visibility = visibility,
                    isExpect = isExpect,
                    modality = modality,
                    isExternal = isExternal,
                    // couldn't be different for getter, possible for set, but it's invalid kotlin
                    isOverride = getter?.overriddenSymbols?.isNotEmpty() == true,
                    isFakeOverride = isFakeOverride,
                    isLateinit = isLateinit,
                    isSuspend = getter?.isSuspend == true,
                    // could be used on property if all accessors have same state, otherwise must be defined on each accessor
                    isInline = false,
                ),
            )
        }

        // TODO don't print borrowed flags and assert that they are same with a setter or don't borrow?
        // TODO we can omit type for set parameter

        p(declaration.isConst, "const")
        p.printWithNoIndent(if (declaration.isVar) "var" else "val")
        p.printWithNoIndent(" ")

        // TODO assert that typeparameters and receiver are ~same for getter ans setter

        declaration.getter?.printTypeParametersWithNoIndent(postfix = " ")

        declaration.getter?.extensionReceiverParameter?.printExtensionReceiverParameter()

        p.printWithNoIndent(declaration.name.asString())

        val backingField = declaration.backingField

        if (backingField != null && !declaration.isDelegated) {
            p.printWithNoIndent(": ")
            backingField.type.printTypeWithNoIndent()
        } else {
            declaration.getter?.returnType?.let {
                p.printWithNoIndent(": ")
                it.printTypeWithNoIndent()
            }
        }

        /* TODO better rendering for
            * field
            * isDelegated
            * delegated w/o backing field
            * provideDelegate
         */

        if (declaration.isDelegated) {
            p.printWithNoIndent(" ", commentBlock("by"))
        }

        p.printlnWithNoIndent()
        p.pushIndent()

        // TODO share code with visitField?
        // it's not valid kotlin
        declaration.backingField?.initializer?.let {
            if (options.bodyPrintingStrategy != BodyPrintingStrategy.NO_BODIES) {
                // If the strategy is PRINT_ONLY_LOCAL_CLASSES_AND_FUNCTIONS, the local declarations in the backing field initializer
                // will be printed under 'field'.
                p.print("field")
            }
            if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
                p.printWithNoIndent(" = ")
            }
            it.accept(this, declaration)
            if (options.bodyPrintingStrategy != BodyPrintingStrategy.NO_BODIES) {
                p.printlnWithNoIndent()
            }
        }

        // TODO generate better name for set parameter `<set-?>`?
        declaration.getter?.printAccessor("get", declaration)
        declaration.setter?.printAccessor("set", declaration)

        p.popIndent()
        p.printlnWithNoIndent()
    }

    private fun IrSimpleFunction.printAccessor(s: String, property: IrDeclaration) {
        val isDefaultAccessor = origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        printSimpleFunction(
            property,
            keyword = "",
            name = s,
            printTypeParametersAndExtensionReceiver = false,
            printSignatureAndBody = isDefaultAccessor,
            printExtraTrailingNewLine = false,
        )
    }

    override fun visitField(declaration: IrField, data: IrDeclaration?) = wrap(declaration, data) {
        declaration.printlnAnnotations()
        p.printIndent()

        declaration.run {
            printModifiersWithNoIndent(
                this,
                Modifiers(
                    visibility = visibility,
                    isExternal = isExternal,
                ),
            )
        }

        if (declaration.isStatic || declaration.isFinal) {
            // it's not valid kotlin unless it's commented
            p.printWithNoIndent(CUSTOM_MODIFIER_START)
            p(declaration.isStatic, "static")
            p(declaration.isFinal, "final")
            p.printWithNoIndent("field")
            p.printWithNoIndent(CUSTOM_MODIFIER_END)
            p.printWithNoIndent(" ")
        }

        p.printWithNoIndent(if (declaration.isFinal) "val " else "var ")
        p.printWithNoIndent(declaration.name.asString() + ": ")
        declaration.type.printTypeWithNoIndent()

        declaration.initializer?.let {
            if (options.bodyPrintingStrategy == BodyPrintingStrategy.PRINT_BODIES) {
                p.printWithNoIndent(" = ")
            }
            it.accept(this, declaration)
        }

        // TODO correspondingPropertySymbol

        p.printlnWithNoIndent()
    }

    override fun visitVariable(declaration: IrVariable, data: IrDeclaration?) = wrap(declaration, data) {
        declaration.printlnAnnotations()
        p.printIndent()

        p(declaration.isLateinit, "lateinit")
        p(declaration.isConst, "const")
        declaration.run { printVariable(isVar, normalizedName(variableNameData), type) }

        declaration.initializer?.let {
            p.printWithNoIndent(" = ")
            it.accept(this, declaration)
        }
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: IrDeclaration?) = wrap(declaration, data) {
        declaration.printlnAnnotations()
        p.printIndent()

        // TODO think about better rendering
        declaration.run { printVariable(isVar, name.asString(), type) }

        p.printlnWithNoIndent()
        p.pushIndent()

        declaration.delegate.accept(this, declaration)
        p.printlnWithNoIndent()

        declaration.getter.printAccessor("get", declaration)
        declaration.setter?.printAccessor("set", declaration)

        p.popIndent()
        p.printlnWithNoIndent()
    }

    private fun printVariable(isVar: Boolean, name: String, type: IrType) {
        p.printWithNoIndent(if (isVar) "var" else "val")
        p.printWithNoIndent(" ")
        p.printWithNoIndent(name)
        p.printWithNoIndent(": ")
        type.printTypeWithNoIndent()
    }

    private fun <Body : IrBody> printBody(body: Body, data: IrDeclaration?, actuallyPrint: () -> Unit) = wrap(body, data) {
        when (options.bodyPrintingStrategy) {
            BodyPrintingStrategy.NO_BODIES -> {}
            BodyPrintingStrategy.PRINT_ONLY_LOCAL_CLASSES_AND_FUNCTIONS -> body.acceptChildren(
                // Don't print bodies, but print local classes and functions declared in those bodies
                object : IrElementVisitor<Unit, IrDeclaration?> {
                    override fun visitElement(element: IrElement, data: IrDeclaration?) {
                        element.acceptChildren(this, data)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
                        p.println()
                        p.pushIndent()
                        declaration.accept(this@KotlinLikeDumper, data)
                        p.popIndent()
                    }

                    override fun visitVariable(declaration: IrVariable, data: IrDeclaration?) {
                        declaration.acceptChildren(this, data)
                    }

                    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: IrDeclaration?) {
                        declaration.acceptChildren(this, data)
                    }
                },
                data
            )
            BodyPrintingStrategy.PRINT_BODIES -> actuallyPrint()
        }
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: IrDeclaration?) {
        printBody(body, data) {
            // TODO should we print something here?
            body.expression.accept(this, data)
        }
    }

    override fun visitBlockBody(body: IrBlockBody, data: IrDeclaration?) {
        printBody(body, data) {
            body.printStatementContainer("{", "}", data)
            p.printlnWithNoIndent()
        }
    }

    override fun visitComposite(expression: IrComposite, data: IrDeclaration?) = wrap(expression, data) {
        expression.printStatementContainer("// COMPOSITE {", "// }", data, withIndentation = false)
    }

    override fun visitBlock(expression: IrBlock, data: IrDeclaration?) = wrap(expression, data) {
        // TODO special blocks using `origin`
        // TODO inlineFunctionSymbol for IrReturnableBlock
        // TODO no tests for IrReturnableBlock?
        val kind = when (expression) {
            is IrReturnableBlock -> "RETURNABLE BLOCK"
            is IrInlinedFunctionBlock -> "INLINED FUNCTION BLOCK"
            else -> "BLOCK"
        }
        // it's not valid kotlin
        expression.printStatementContainer("{ // $kind", "}", data)
    }

    private fun IrStatementContainer.printStatementContainer(
        before: String,
        after: String,
        data: IrDeclaration?,
        withIndentation: Boolean = true
    ) {
        // TODO type for IrContainerExpression
        p.printlnWithNoIndent(before)
        if (withIndentation) p.pushIndent()

        statements.forEach {
            if (it is IrExpression) p.printIndent()
            it.accept(this@KotlinLikeDumper, data)
            p.printlnWithNoIndent()
        }

        if (withIndentation) p.popIndent()
        p.print(after)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody, data: IrDeclaration?) {
        printBody(body, data) {
            // it's not valid kotlin
            p.printlnWithNoIndent("/* Synthetic body for ${body.kind} */")
        }
    }

    override fun visitCall(expression: IrCall, data: IrDeclaration?) = wrap(expression, data) {
        // TODO process specially builtin symbols
        expression.printMemberAccessExpressionWithNoIndent(
            expression.symbol.safeName,
            expression.symbol.safeValueParameters,
            expression.superQualifierSymbol,
            omitAllBracketsIfNoArguments = false,
            data = data,
        )
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrDeclaration?) = wrap(expression, data) {
        expression.printMemberAccessExpressionWithNoIndent(
            expression.symbol.safeParentClassName,
            expression.symbol.safeValueParameters,
            superQualifierSymbol = null,
            omitAllBracketsIfNoArguments = expression.symbol.safeParentClassOrNull?.isAnnotationClass == true,
            data = data,
        )
    }

    private fun IrMemberAccessExpression<*>.printMemberAccessExpressionWithNoIndent(
        name: String,
        valueParameters: List<IrValueParameter>,
        superQualifierSymbol: IrClassSymbol?,
        omitAllBracketsIfNoArguments: Boolean,
        data: IrDeclaration?,
        accessOperator: String = ".",
        omitAccessOperatorIfNoReceivers: Boolean = true,
        wrapArguments: Boolean = false
    ) {
        // TODO origin

        val twoReceivers =
            (dispatchReceiver != null || superQualifierSymbol != null) && extensionReceiver != null

        if (twoReceivers) {
            p.printWithNoIndent("(")
        }

        superQualifierSymbol?.let {
            // TODO which supper? smart mode?
            p.printWithNoIndent("super<${it.safeName}>")
        }

        dispatchReceiver?.let {
            if (superQualifierSymbol == null) it.accept(this@KotlinLikeDumper, data)
            // else assert dispatchReceiver === this
        }
        // it's not valid kotlin
        p(twoReceivers, ",")
        extensionReceiver?.accept(this@KotlinLikeDumper, data)
        if (twoReceivers) {
            p.printWithNoIndent(")")
        }


        if (!omitAccessOperatorIfNoReceivers ||
            (dispatchReceiver != null || extensionReceiver != null || superQualifierSymbol != null)
        ) {
            p.printWithNoIndent(accessOperator)
        }

        p.printWithNoIndent(name)

        fun allValueArgumentsAreNull(): Boolean {
            for (i in 0 until valueArgumentsCount) {
                if (getValueArgument(i) != null) return false
            }
            return true
        }

        if (omitAllBracketsIfNoArguments && typeArgumentsCount == 0 && (valueArgumentsCount == 0 || allValueArgumentsAreNull())) return

        if (wrapArguments) p.printWithNoIndent("/*")

        if (typeArgumentsCount > 0) {
            p.printWithNoIndent("<")
            repeat(typeArgumentsCount) {
                p(it > 0, ",")
                // TODO flag to print type param name?
                getTypeArgument(it)?.printTypeWithNoIndent() ?: p.printWithNoIndent(commentBlock("null"))
            }
            p.printWithNoIndent(">")
        }

        p.printWithNoIndent("(")

// TODO introduce a flag to print receiver this way?
//        // it's not valid kotlin
//        expression.extensionReceiver?.let {
//            p.printWithNoIndent("\$receiver = ")
//            it.acceptVoid(this)
//            if (expression.valueArgumentsCount > 0) p.printWithNoIndent(", ")
//        }

        repeat(valueArgumentsCount) { i ->
            // TODO should we print something for omitted arguments (== null)?
            getValueArgument(i)?.let {
                p(i > 0, ",")
                // TODO flag to print param name
                // If the symbol is unbound then valueArgumentsCount disagrees with
                // valueParameters.
                if (i < valueParameters.size) {
                    p.printWithNoIndent(valueParameters[i].name.asString() + " = ")
                }
                it.accept(this@KotlinLikeDumper, data)
            }
        }

        p.printWithNoIndent(")")
        if (wrapArguments) p.printWithNoIndent("*/")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrDeclaration?) = wrap(expression, data) {
        // TODO skip call to Any?
        expression.printConstructorCallWithNoIndent(data)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: IrDeclaration?) = wrap(expression, data) {
        // TODO skip call to Enum?
        expression.printConstructorCallWithNoIndent(data)
    }

    private fun IrFunctionAccessExpression.printConstructorCallWithNoIndent(
        data: IrDeclaration?
    ) {
        // TODO flag to omit comment block?
        val delegatingClass = symbol.safeParentClassOrNull
        val currentClass = data?.parent as? IrClass
        val delegatingClassName = symbol.safeParentClassName

        val name = if (data is IrConstructor) {
            when (currentClass) {
                // it's not valid kotlin, it's fallback for the case when data wasn't provided
                null -> "delegating/*$delegatingClassName*/"
                delegatingClass -> "this/*$delegatingClassName*/"
                else -> "super/*$delegatingClassName*/"
            }
        } else {
            delegatingClassName // required only for IrEnumConstructorCall
        }

        printMemberAccessExpressionWithNoIndent(
            name,
            symbol.safeValueParameters,
            superQualifierSymbol = null,
            omitAllBracketsIfNoArguments = false,
            data = data,
        )
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: IrDeclaration?) = wrap(expression, data) {
        // TODO assert that `expression.classSymbol.owner == data.parentAsClass
        // TODO better rendering
        p.printlnWithNoIndent("/* <init>() */")
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: IrDeclaration?) {
        // TODO omit the name when it's possible
        // TODO Is there a difference between `<anonymous>` and `<no name provided>`?
        // TODO Is name of function used somewhere? How it's important?
        // TODO Use lambda syntax when possible
        // TODO don't print visibility?
        p.withholdIndentOnce()
        expression.function.printSimpleFunction(
            data,
            "fun ",
            expression.function.name.asString(),
            printTypeParametersAndExtensionReceiver = true,
            printSignatureAndBody = true,
            printExtraTrailingNewLine = false,
        )
    }

    override fun visitGetField(expression: IrGetField, data: IrDeclaration?) = wrap(expression, data) {
        expression.printFieldAccess(data)
    }

    override fun visitSetField(expression: IrSetField, data: IrDeclaration?) = wrap(expression, data) {
        expression.printFieldAccess(data)
        p.printWithNoIndent(" = ")
        expression.value.accept(this, data)
    }

    private fun IrFieldAccessExpression.printFieldAccess(data: IrDeclaration?) {
        // it's not valid kotlin
        receiver?.accept(this@KotlinLikeDumper, data)
        superQualifierSymbol?.let {
            // TODO which supper? smart mode?
            // it's not valid kotlin
            if (receiver != null) p.printWithNoIndent("(")
            p.printWithNoIndent("super<${it.safeName}>")
            if (receiver != null) p.printWithNoIndent(")")
        }

        if (receiver != null || superQualifierSymbol != null) {
            p.printWithNoIndent(".")
        }

        // it's not valid kotlin
        p.printWithNoIndent("#" + symbol.safeName)
    }

    override fun visitGetValue(expression: IrGetValue, data: IrDeclaration?) = wrap(expression, data) {
        p.printWithNoIndent(expression.symbol.safeName)
    }

    override fun visitSetValue(expression: IrSetValue, data: IrDeclaration?) = wrap(expression, data) {
        p.printWithNoIndent(expression.symbol.safeName + " = ")
        expression.value.accept(this, data)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: IrDeclaration?) = wrap(expression, data) {
        // TODO what if symbol is unbound?
        expression.symbol.defaultType.printTypeWithNoIndent()
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: IrDeclaration?) = wrap(expression, data) {
        p.printWithNoIndent(expression.symbol.safeParentClassName)
        p.printWithNoIndent(".")
        p.printWithNoIndent(expression.symbol.safeName)
    }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: IrDeclaration?) = wrap(expression, data) {
        // TODO support
        // TODO no test
        // it's not valid kotlin
        p.printWithNoIndent("&")
        super.visitRawFunctionReference(expression, data)
    }

    override fun visitReturn(expression: IrReturn, data: IrDeclaration?) = wrap(expression, data) {
        // TODO label
        // TODO optionally don't print Unit when return type of returnTargetSymbol is Unit
        p.printWithNoIndent("return ")
        expression.value.accept(this, data)
    }

    override fun visitThrow(expression: IrThrow, data: IrDeclaration?) {
        p.printWithNoIndent("throw ")
        expression.value.accept(this, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrDeclaration?) = wrap(expression, data) {
        // TODO use triple quotes when possible?
        // TODO optionally each argument at a separate line, another option add a wrapping
        expression.arguments.forEachIndexed { i, e ->
            p(i > 0, " +")
            e.accept(this, data)
        }
    }

    override fun visitConst(expression: IrConst<*>, data: IrDeclaration?) = wrap(expression, data) {
        val kind = expression.kind

        val (prefix, postfix) = when (kind) {
            is IrConstKind.Null -> "" to ""
            is IrConstKind.Boolean -> "" to ""
            is IrConstKind.Char -> "'" to "'"
            // it's not valid kotlin
            is IrConstKind.Byte -> "" to "B"
            // it's not valid kotlin
            is IrConstKind.Short -> "" to "S"
            is IrConstKind.Int -> "" to ""
            is IrConstKind.Long -> "" to "L"
            is IrConstKind.String -> "\"" to "\""
            is IrConstKind.Float -> "" to "F"
            is IrConstKind.Double -> "" to ""
        }

        val value = expression.value.toString()
        val safeValue = when (kind) {
            // TODO no tests for escaping quotes (',")
            is IrConstKind.Char -> StringUtil.escapeCharCharacters(value)
            is IrConstKind.String -> StringUtil.escapeStringCharacters(value)
            else -> value
        }

        p.printWithNoIndent(prefix, safeValue, postfix)
    }

    override fun visitVararg(expression: IrVararg, data: IrDeclaration?) = wrap(expression, data) {
        // TODO better rendering?
        // TODO varargElementType
        // it's not valid kotlin
        p.printWithNoIndent("[")
        expression.elements.forEachIndexed { i, e ->
            p(i > 0, ",")
            e.accept(this, data)
        }
        p.printWithNoIndent("]")
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: IrDeclaration?) = wrap(spread, data) {
        p.printWithNoIndent("*")
        spread.expression.accept(this, data)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclaration?) = wrap(expression, data) {
        val (operator, after) = when (expression.operator) {
            IrTypeOperator.CAST -> "as" to ""
            IrTypeOperator.IMPLICIT_CAST -> "/*as" to " */"
            IrTypeOperator.IMPLICIT_NOTNULL -> "/*!!" to " */"
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> "/*~>" to " */"
            IrTypeOperator.SAFE_CAST -> "as?" to ""
            IrTypeOperator.INSTANCEOF -> "is" to ""
            IrTypeOperator.NOT_INSTANCEOF -> "!is" to ""
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> "/*~>" to " */"
            IrTypeOperator.SAM_CONVERSION -> "/*->" to " */"
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> "/*~>" to " */"
            IrTypeOperator.REINTERPRET_CAST -> "/*=>" to " */"
        }

        expression.argument.accept(this, data)
        p.printWithNoIndent(" $operator ")
        expression.typeOperand.printTypeWithNoIndent()
        p.printWithNoIndent(after)

    }

    override fun visitWhen(expression: IrWhen, data: IrDeclaration?) = wrap(expression, data) {
        // TODO print if when possible?
        p.printlnWithNoIndent("when {")
        p.pushIndent()

        expression.branches.forEach { it.accept(this, data) }

        p.popIndent()
        p.print("}")
    }

    override fun visitBranch(branch: IrBranch, data: IrDeclaration?) = wrap(branch, data) {
        p.printIndent()
        branch.condition.accept(this, data)
        p.printWithNoIndent(" -> ")
        branch.result.accept(this, data)
        p.println()
    }

    override fun visitElseBranch(branch: IrElseBranch, data: IrDeclaration?) = wrap(branch, data) {
        p.printIndent()
        if ((branch.condition as? IrConst<*>)?.value == true) {
            p.printWithNoIndent(if (options.printElseAsTrue) "true" else "else")
        } else {
            p.printWithNoIndent("/* else */ ")
            branch.condition.accept(this, data)
        }
        p.printWithNoIndent(" -> ")
        branch.result.accept(this, data)
        p.println()
    }

    private fun IrLoop.printLabel() {
        label?.let {
            p.printWithNoIndent(it)
            p.printWithNoIndent("@ ")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: IrDeclaration?) = wrap(loop, data) {
        loop.printLabel()

        p.printWithNoIndent("while (")
        loop.condition.accept(this, data)
        p.printWithNoIndent(") ")

        loop.body?.accept(this, data)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: IrDeclaration?) = wrap(loop, data) {
        loop.printLabel()

        p.printWithNoIndent("do")

        loop.body?.accept(this, data)

        p.print("while (")
        loop.condition.accept(this, data)
        p.printWithNoIndent(")")

    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: IrDeclaration?) = wrap(jump, data) {
        // TODO render loop reference
        p.printWithNoIndent(if (jump is IrContinue) "continue" else "break")
        jump.label?.let {
            p.printWithNoIndent("@")
            p.printWithNoIndent(it)
        }
    }

    override fun visitTry(aTry: IrTry, data: IrDeclaration?) = wrap(aTry, data) {
        p.printWithNoIndent("try ")
        aTry.tryResult.accept(this, data)
        p.printlnWithNoIndent()

        aTry.catches.forEach { it.accept(this, data) }

        aTry.finallyExpression?.let {
            p.print("finally ")
            it.accept(this, data)
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: IrDeclaration?) = wrap(aCatch, data) {
        p.print("catch (")
        aCatch.catchParameter.run {
            p.printWithNoIndent(name.asString())
            p.printWithNoIndent(": ")
            type.printTypeWithNoIndent()
        }
        p.printWithNoIndent(")")
        aCatch.result.accept(this, data)
        p.printlnWithNoIndent()
    }

    override fun visitGetClass(expression: IrGetClass, data: IrDeclaration?) = wrap(expression, data) {
        expression.argument.accept(this, data)
        p.printWithNoIndent("::class")
    }

    override fun visitClassReference(expression: IrClassReference, data: IrDeclaration?) = wrap(expression, data) {
        // TODO use classType
        p.printWithNoIndent(expression.symbol.safeName)
        p.printWithNoIndent("::class")
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclaration?) = wrap(expression, data) {
        // TODO reflectionTarget
        expression.printCallableReferenceWithNoIndent(expression.symbol.safeValueParameters, data)
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: IrDeclaration?) = wrap(expression, data) {
        // TODO do we need additional fields (field, getter, setter)?
        expression.printCallableReferenceWithNoIndent(emptyList(), data)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: IrDeclaration?) =
        wrap(expression, data) {
            // TODO do we need additional fields (delegate, getter, setter)?
            expression.printCallableReferenceWithNoIndent(emptyList(), data)
        }

    private fun IrCallableReference<*>.printCallableReferenceWithNoIndent(valueParameters: List<IrValueParameter>, data: IrDeclaration?) {
        // TODO where from to get type arguments for a class?
        // TODO rendering for references to constructors
        if (dispatchReceiver == null && extensionReceiver == null) {
            symbol.safeParentClassOrNull?.let {
                p.printWithNoIndent(it.name.asString())
            }
        }

        printMemberAccessExpressionWithNoIndent(
            (symbol.owner as IrDeclarationWithName).name.asString(),
            valueParameters,
            superQualifierSymbol = null,
            omitAllBracketsIfNoArguments = true,
            data = data,
            accessOperator = "::",
            omitAccessOperatorIfNoReceivers = false,
            wrapArguments = true
        )
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: IrDeclaration?) = wrap(expression, data) {
        // TODO marker to show that it's dynamic call
        val s = when (val op = expression.operator) {
            IrDynamicOperator.ARRAY_ACCESS -> "[" to "]"
            IrDynamicOperator.INVOKE -> "(" to ")"

            // assert that arguments size is 0
            IrDynamicOperator.UNARY_PLUS,
            IrDynamicOperator.UNARY_MINUS,
            IrDynamicOperator.EXCL,
            IrDynamicOperator.PREFIX_INCREMENT,
            IrDynamicOperator.PREFIX_DECREMENT -> {
                p.printWithNoIndent(op.image)
                "" to ""
            }

            // assert that arguments size is 0
            IrDynamicOperator.POSTFIX_INCREMENT,
            IrDynamicOperator.POSTFIX_DECREMENT -> {
                op.image to ""
            }

            // assert that arguments size is 1
            else -> " ${op.image} " to ""
        }

        expression.receiver.accept(this, data)
        p.printWithNoIndent(s.first)
        expression.arguments.forEachIndexed { i, e ->
            p(i > 0, ",")
            e.accept(this, data)
        }
        p.printWithNoIndent(s.second)
    }

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: IrDeclaration?) = wrap(expression, data) {
        expression.receiver.accept(this, data)
        p.printWithNoIndent(".")
        p.printWithNoIndent(expression.memberName)
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: IrDeclaration?) = wrap(declaration, data) {
        // TODO declaration.printlnAnnotations()
        p.println("/* ErrorDeclaration */")
    }

    override fun visitErrorExpression(expression: IrErrorExpression, data: IrDeclaration?) = wrap(expression, data) {
        // TODO description
        p.printWithNoIndent("error(\"\") /* ErrorExpression */")
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: IrDeclaration?) = wrap(expression, data) {
        // TODO description
        // TODO better rendering
        p.printWithNoIndent("error(\"\") /* ErrorCallExpression */")
        expression.explicitReceiver?.let {
            it.accept(this, data)
            p.printWithNoIndent("; ")
        }
        expression.arguments.forEach { arg ->
            arg.accept(this, data)
            p.printWithNoIndent("; ")
        }
    }

    private fun p(condition: Boolean, s: String) {
        if (condition) p.printWithNoIndent("$s ")
    }

    private fun <T : Any> p(value: T?, defaultValue: T? = null, getString: T.() -> String) {
        if (value == null) return
        p(value != defaultValue, value.getString())
    }

    private fun commentBlock(text: String) = "/* $text */"

    // it's not valid kotlin unless it's commented
    //  ^^^ it's applied to all usages of this function
    private fun customModifier(text: String): String {
        return CUSTOM_MODIFIER_START + text + CUSTOM_MODIFIER_END
    }

    private companion object {
        private const val CUSTOM_MODIFIER_START = "/* "
        private const val CUSTOM_MODIFIER_END = " */"
    }
}
