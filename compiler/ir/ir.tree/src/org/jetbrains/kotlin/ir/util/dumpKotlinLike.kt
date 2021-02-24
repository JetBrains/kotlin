/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
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

class KotlinLikeDumpOptions(
    val printRegionsPerFile: Boolean = false,
    val printFileName: Boolean = true,
    val printFilePath: Boolean = true,
    // TODO support
    val useNamedArguments: Boolean = false,
    // TODO support
    val labelPrintingStrategy: LabelPrintingStrategy = LabelPrintingStrategy.NEVER,
    val printFakeOverridesStrategy: FakeOverridesStrategy = FakeOverridesStrategy.ALL,
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

// TODO_ conventions:
// TODO support -- for unsupported nodes
// TODO no test -- for the cases with no test(s)
// it's not valid kotlin -- for the cases when used some syntax which is invalid in Kotlin, maybe they are worth to reconsider

/* TODO:
    * don't crash on unbound symbols
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
    * "normalize" names for tmps? ^^ Could unique ids help?
    * wrap/escape invalid identifiers with "`", like "$$delegate"
 */

private class KotlinLikeDumper(val p: Printer, val options: KotlinLikeDumpOptions) : IrElementVisitor<Unit, IrDeclaration?> {
    fun printElement(element: IrElement) {
        element.accept(this, null)
    }

    fun printType(type: IrType) {
        type.printTypeWithNoIndent()
    }

    fun printTypeArgument(typeArg: IrTypeArgument) {
        typeArg.printTypeArgumentWithNoIndent()
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

    override fun visitModuleFragment(declaration: IrModuleFragment, data: IrDeclaration?) {
        p.println("// MODULE: ${declaration.name.asString()}")
        declaration.acceptChildren(this, null)
    }

    override fun visitFile(declaration: IrFile, data: IrDeclaration?) {
        if (options.printRegionsPerFile) p.println("//region block: ${declaration.name}")

        if (options.printFileName) p.println("// FILE: ${declaration.name}")
        if (options.printFilePath) p.println("// path: ${declaration.path}")
        declaration.printlnAnnotations("file")
        val packageFqName = declaration.packageFragmentDescriptor.fqName
        if (!packageFqName.isRoot) {
            p.println("package ${packageFqName.asString()}")
        }
        if (!p.isEmpty) p.printlnWithNoIndent()

        declaration.declarations.forEach { it.accept(this, null) }

        if (options.printRegionsPerFile) p.println("//endregion")
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment, data: IrDeclaration?) {
        // TODO support
        super.visitExternalPackageFragment(declaration, data)
    }

    override fun visitScript(declaration: IrScript, data: IrDeclaration?) {
        // TODO support
        super.visitScript(declaration, data)
    }

    override fun visitClass(declaration: IrClass, data: IrDeclaration?) {
        // TODO omit super class for enums, annotations?
        // TODO omit Companion name for companion objects?
        // TODO do we need to print info about `thisReceiver`?
        // TODO special support for objects?

        declaration.printlnAnnotations()
        p.printIndent()

        declaration.run {
            printModifiersWithNoIndent(
                visibility,
                isExpect,
                modality,
                isExternal,
                isOverride = INAPPLICABLE,
                isFakeOverride = INAPPLICABLE,
                isLateinit = INAPPLICABLE,
                isTailrec = INAPPLICABLE,
                isVararg = INAPPLICABLE,
                isSuspend = INAPPLICABLE,
                isInner,
                isInline,
                isData,
                isCompanion,
                isFun,
                kind,
                isInfix = INAPPLICABLE,
                isOperator = INAPPLICABLE,
                isInterfaceMember = INAPPLICABLE
            )
        }

        p.printWithNoIndent(declaration.name.asString())

        declaration.printTypeParametersWithNoIndent()
        // TODO no test
        if (declaration.superTypes.isNotEmpty()) {
            var first = true
            for (type in declaration.superTypes) {
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

        declaration.declarations.forEach { it.accept(this, declaration) }

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
    infix
    operator
    data
     */
    private fun printModifiersWithNoIndent(
        visibility: DescriptorVisibility,
        isExpect: Boolean,
        modality: Modality?,
        isExternal: Boolean,
        isOverride: Boolean,
        isFakeOverride: Boolean,
        isLateinit: Boolean,
        isTailrec: Boolean,
        isVararg: Boolean,
        isSuspend: Boolean,
        isInner: Boolean,
        isInline: Boolean,
        isData: Boolean,
        isCompanion: Boolean,
        isFunInterface: Boolean,
        classKind: ClassKind?,
        isInfix: Boolean,
        isOperator: Boolean,
        isInterfaceMember: Boolean,
    ) {
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
        p(modality, defaultModality) { name.toLowerCase() }
        p(isExternal, "external")
        p(isFakeOverride, customModifier("fake"))
        p(isOverride, "override")
        p(isLateinit, "lateinit")
        p(isTailrec, "tailrec")
        printParameterModifiersWithNoIndent(
            isVararg,
            isCrossinline = INAPPLICABLE,
            isNoinline = INAPPLICABLE,
            isHidden = INAPPLICABLE,
            isAssignable = INAPPLICABLE
        )
        p(isSuspend, "suspend")
        p(isInner, "inner")
        p(isInline, "inline")
        p(isData, "data")
        p(isCompanion, "companion")
        p(isFunInterface, "fun")
        p(classKind) { name.toLowerCase().replace('_', ' ') + if (this == ClassKind.ENUM_ENTRY) " class" else "" }
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

    private fun IrAnnotationContainer.printAnnotationsWithNoIndent() {
        annotations.forEach {
            it.printAnAnnotationWithNoIndent()
            p.printWithNoIndent(" ")
        }
    }

    private fun IrAnnotationContainer.printlnAnnotations(prefix: String = "") {
        annotations.forEach {
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
        superTypes.forEach { type ->
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

                p.printWithNoIndent((classifier.owner as IrDeclarationWithName).name.asString())

                if (arguments.isNotEmpty()) {
                    p.printWithNoIndent("<")
                    arguments.forEachIndexed { i, typeArg ->
                        p(i > 0, ",")

                        typeArg.printTypeArgumentWithNoIndent()
                    }
                    p.printWithNoIndent(">")
                }

                if (hasQuestionMark) p.printWithNoIndent("?")
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

    override fun visitTypeAlias(declaration: IrTypeAlias, data: IrDeclaration?) {
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

    override fun visitEnumEntry(declaration: IrEnumEntry, data: IrDeclaration?) {
        // TODO better rendering for enum entries

        declaration.correspondingClass?.let { p.println() }

        declaration.printlnAnnotations()
        p.printIndent()
        p.printWithNoIndent(declaration.name)
        declaration.initializerExpression?.let {
            // it's not valid kotlin
            p.printWithNoIndent(" = ")
            it.accept(this, declaration)
        }
        p.println()

        declaration.correspondingClass?.accept(this, declaration)

        p.println()
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: IrDeclaration?) {
        // TODO no tests, looks like IrAnonymousInitializer has annotations accidentally.
        declaration.printlnAnnotations()
        p.printIndent()

        // TODO no tests, looks like there are no irText tests for isStatic flag
        p(declaration.isStatic, customModifier("static"))
        p.printWithNoIndent("init ")
        declaration.body.accept(this, declaration)

        p.printlnWithNoIndent()
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: IrDeclaration?) {
        declaration.printSimpleFunction(
            "fun ",
            declaration.name.asString(),
            printTypeParametersAndExtensionReceiver = true,
            printSignatureAndBody = true
        )
        p.printlnWithNoIndent()
    }

    override fun visitConstructor(declaration: IrConstructor, data: IrDeclaration?) {
        // TODO name?
        // TODO is it worth to merge code for IrConstructor and IrSimpleFunction?
        // TODO dispatchReceiverParameter -- outer `this` for inner classes
        // TODO return type?

        declaration.printlnAnnotations()
        p.printIndent()

        declaration.run {
            printModifiersWithNoIndent(
                visibility,
                isExpect,
                modality = INAPPLICABLE_N,
                isExternal,
                isOverride = INAPPLICABLE,
                isFakeOverride = INAPPLICABLE,
                isLateinit = INAPPLICABLE,
                isTailrec = INAPPLICABLE,
                isVararg = INAPPLICABLE,
                isSuspend = INAPPLICABLE,
                isInner = INAPPLICABLE,
                isInline,
                isData = INAPPLICABLE,
                isCompanion = INAPPLICABLE,
                isFunInterface = INAPPLICABLE,
                classKind = INAPPLICABLE_N,
                isInfix = INAPPLICABLE,
                isOperator = INAPPLICABLE,
                isInterfaceMember = INAPPLICABLE
            )
        }

        p.printWithNoIndent("constructor")
        declaration.printTypeParametersWithNoIndent()
        declaration.printValueParametersWithNoIndent()
        declaration.printWhereClauseIfNeededWithNoIndent()
        p.printWithNoIndent(" ")
        p(declaration.isPrimary, customModifier("primary"))
        declaration.body?.accept(this, declaration)
        p.printlnWithNoIndent()
    }

    private fun IrSimpleFunction.printSimpleFunction(
        keyword: String,
        name: String,
        printTypeParametersAndExtensionReceiver: Boolean,
        printSignatureAndBody: Boolean
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

        printlnAnnotations()
        p.print("")

        run {
            printModifiersWithNoIndent(
                visibility,
                isExpect,
                modality,
                isExternal,
                isOverride = overriddenSymbols.isNotEmpty(),
                isFakeOverride,
                isLateinit = INAPPLICABLE,
                isTailrec,
                isVararg = INAPPLICABLE,
                isSuspend,
                isInner = INAPPLICABLE,
                isInline,
                isData = INAPPLICABLE,
                isCompanion = INAPPLICABLE,
                isFunInterface = INAPPLICABLE,
                classKind = INAPPLICABLE_N,
                isInfix,
                isOperator,
                isInterfaceMember = (this@printSimpleFunction.parent as? IrClass)?.isInterface == true
            )
        }

        p.printWithNoIndent(keyword)

        if (printTypeParametersAndExtensionReceiver) printTypeParametersWithNoIndent(postfix = " ")

        if (printTypeParametersAndExtensionReceiver) {
            extensionReceiverParameter?.printExtensionReceiverParameter()
        }

        p.printWithNoIndent(name)

        if (printSignatureAndBody) {
            printValueParametersWithNoIndent()

            if (!returnType.isUnit()) {
                p.printWithNoIndent(": ")
                returnType.printTypeWithNoIndent()
            }
            printWhereClauseIfNeededWithNoIndent()
            p.printWithNoIndent(" ")

            body?.accept(this@KotlinLikeDumper, null)
        } else {
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
            p.printWithNoIndent(" = ")
            v.accept(this@KotlinLikeDumper, data)
        }
    }

    override fun visitTypeParameter(declaration: IrTypeParameter, data: IrDeclaration?) {
        declaration.printATypeParameterWithNoIndent()
        if (declaration.superTypes.size > 1) declaration.printWhereClauseTypesWithNoIndent(true)
    }

    override fun visitValueParameter(declaration: IrValueParameter, data: IrDeclaration?) {
        // TODO index?
        declaration.printAValueParameterWithNoIndent(data)
    }

    override fun visitProperty(declaration: IrProperty, data: IrDeclaration?) {
        if (options.printFakeOverridesStrategy == FakeOverridesStrategy.NONE && declaration.isFakeOverride) return

        declaration.printlnAnnotations()
        p.printIndent()

        // TODO better rendering for modifiers on property and accessors
        //  modifiers that could be different between accessors and property have a comment
        declaration.run {
            printModifiersWithNoIndent(
                // accessors by default have same visibility, but the can define own value
                visibility,
                isExpect,
                modality,
                isExternal,
                // couldn't be different for getter, possible for set, but it's invalid kotlin
                isOverride = getter?.overriddenSymbols?.isNotEmpty() == true,
                isFakeOverride,
                isLateinit,
                isTailrec = INAPPLICABLE,
                isVararg = INAPPLICABLE,
                isSuspend = getter?.isSuspend == true,
                isInner = INAPPLICABLE,
                // could be used on property if all all accessors have same state, otherwise must be defined on each accessor
                isInline = false,
                isData = INAPPLICABLE,
                isCompanion = INAPPLICABLE,
                isFunInterface = INAPPLICABLE,
                classKind = INAPPLICABLE_N,
                isInfix = INAPPLICABLE,
                isOperator = INAPPLICABLE,
                isInterfaceMember = (declaration.parent as? IrClass)?.isInterface == true
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

        declaration.getter?.returnType?.let {
            p.printWithNoIndent(": ")
            it.printTypeWithNoIndent()
        }

        /* TODO better rendering for
            * field
            * isDelegated
            * delegated w/o backing field
            * provideDelegate
         */

        p(declaration.isDelegated, " " + commentBlock("by"))

        p.printlnWithNoIndent()
        p.pushIndent()

        // TODO share code with visitField?
        // it's not valid kotlin
        declaration.backingField?.initializer?.let {
            p.print("field = ")
            it.accept(this, declaration)
            p.printlnWithNoIndent()
        }

        // TODO generate better name for set parameter `<set-?>`?
        declaration.getter?.printAccessor("get")
        declaration.setter?.printAccessor("set")

        p.popIndent()
        p.printlnWithNoIndent()
    }

    private fun IrSimpleFunction.printAccessor(s: String) {
        val isDefaultAccessor = origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        printSimpleFunction("", s, printTypeParametersAndExtensionReceiver = false, printSignatureAndBody = isDefaultAccessor)
    }

    override fun visitField(declaration: IrField, data: IrDeclaration?) {
        declaration.printlnAnnotations()
        p.printIndent()

        declaration.run {
            printModifiersWithNoIndent(
                visibility,
                isExpect = INAPPLICABLE,
                modality = INAPPLICABLE_N,
                isExternal,
                isOverride = INAPPLICABLE,
                isFakeOverride = INAPPLICABLE,
                isLateinit = INAPPLICABLE,
                isTailrec = INAPPLICABLE,
                isVararg = INAPPLICABLE,
                isSuspend = INAPPLICABLE,
                isInner = INAPPLICABLE,
                isInline = INAPPLICABLE,
                isData = INAPPLICABLE,
                isCompanion = INAPPLICABLE,
                isFunInterface = INAPPLICABLE,
                classKind = INAPPLICABLE_N,
                isInfix = INAPPLICABLE,
                isOperator = INAPPLICABLE,
                isInterfaceMember = (declaration.parent as? IrClass)?.isInterface == true
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
            p.printWithNoIndent(" = ")
            it.accept(this, declaration)
        }

        // TODO correspondingPropertySymbol

        p.printlnWithNoIndent()
    }

    override fun visitVariable(declaration: IrVariable, data: IrDeclaration?) {
        declaration.printlnAnnotations()
        p.printIndent()

        p(declaration.isLateinit, "lateinit")
        p(declaration.isConst, "const")
        declaration.run { printVariable(isVar, name, type) }

        declaration.initializer?.let {
            p.printWithNoIndent(" = ")
            it.accept(this, declaration)
        }
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: IrDeclaration?) {
        declaration.printlnAnnotations()
        p.printIndent()

        // TODO think about better rendering
        declaration.run { printVariable(isVar, name, type) }

        p.printlnWithNoIndent()
        p.pushIndent()

        declaration.delegate.accept(this, declaration)
        p.printlnWithNoIndent()

        declaration.getter.printAccessor("get")
        declaration.setter?.printAccessor("set")

        p.popIndent()
        p.printlnWithNoIndent()
    }

    private fun printVariable(isVar: Boolean, name: Name, type: IrType) {
        p.printWithNoIndent(if (isVar) "var" else "val")
        p.printWithNoIndent(" ")
        p.printWithNoIndent(name.asString())
        p.printWithNoIndent(": ")
        type.printTypeWithNoIndent()
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: IrDeclaration?) {
        // TODO should we print something here?
        body.expression.accept(this, data)
    }

    override fun visitBlockBody(body: IrBlockBody, data: IrDeclaration?) {
        body.printStatementContainer("{", "}", data)
        p.printlnWithNoIndent()
    }

    override fun visitComposite(expression: IrComposite, data: IrDeclaration?) {
        expression.printStatementContainer("// COMPOSITE {", "// }", data, withIndentation = false)
    }

    override fun visitBlock(expression: IrBlock, data: IrDeclaration?) {
        // TODO special blocks using `origin`
        // TODO inlineFunctionSymbol for IrReturnableBlock
        // TODO no tests for IrReturnableBlock?
        val kind = if (expression is IrReturnableBlock) "RETURNABLE BLOCK" else "BLOCK"
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
        // it's not valid kotlin
        p.printlnWithNoIndent("/* Synthetic body for ${body.kind} */")
    }

    override fun visitCall(expression: IrCall, data: IrDeclaration?) {
        // TODO process specially builtin symbols
        expression.printMemberAccessExpressionWithNoIndent(
            expression.symbol.owner.name.asString(),
            expression.symbol.owner.valueParameters,
            expression.superQualifierSymbol,
            omitAllBracketsIfNoArguments = false,
            data = data,
        )
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrDeclaration?) {
        val clazz = expression.symbol.owner.parentAsClass
        expression.printMemberAccessExpressionWithNoIndent(
            clazz.name.asString(),
            expression.symbol.owner.valueParameters,
            superQualifierSymbol = null,
            omitAllBracketsIfNoArguments = clazz.isAnnotationClass,
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
            // TODO should we print super classifier somehow?
            // TODO which supper? smart mode?
            p.printWithNoIndent("super")
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
                p.printWithNoIndent(valueParameters[i].name.asString() + " = ")
                it.accept(this@KotlinLikeDumper, data)
            }
        }

        p.printWithNoIndent(")")
        if (wrapArguments) p.printWithNoIndent("*/")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrDeclaration?) {
        // TODO skip call to Any?
        expression.printConstructorCallWithNoIndent(data)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: IrDeclaration?) {
        // TODO skip call to Enum?
        expression.printConstructorCallWithNoIndent(data)
    }

    private fun IrFunctionAccessExpression.printConstructorCallWithNoIndent(
        data: IrDeclaration?
    ) {
        // TODO flag to omit comment block?
        val delegatingClass = symbol.owner.parentAsClass
        val currentClass = data?.parent as? IrClass
        val delegatingClassName = delegatingClass.name.asString()

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
            symbol.owner.valueParameters,
            superQualifierSymbol = null,
            omitAllBracketsIfNoArguments = false,
            data = data,
        )
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: IrDeclaration?) {
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
            "fun ",
            expression.function.name.asString(),
            printTypeParametersAndExtensionReceiver = true,
            printSignatureAndBody = true
        )
    }

    override fun visitGetField(expression: IrGetField, data: IrDeclaration?) {
        expression.printFieldAccess(data)
    }

    override fun visitSetField(expression: IrSetField, data: IrDeclaration?) {
        expression.printFieldAccess(data)
        p.printWithNoIndent(" = ")
        expression.value.accept(this, data)
    }

    private fun IrFieldAccessExpression.printFieldAccess(data: IrDeclaration?) {
        // it's not valid kotlin
        receiver?.accept(this@KotlinLikeDumper, data)
        superQualifierSymbol?.let {
            // TODO should we print super classifier somehow?
            // TODO which supper? smart mode?
            // TODO super and receiver at the same time:
            //  compiler/testData/ir/irText/types/smartCastOnFieldReceiverOfGenericType.kt
            // it's not valid kotlin
            p.printWithNoIndent("super")
        }

        if (receiver != null || superQualifierSymbol != null) {
            p.printWithNoIndent(".")
        }

        // it's not valid kotlin
        p.printWithNoIndent("#" + symbol.owner.name.asString())
    }

    override fun visitGetValue(expression: IrGetValue, data: IrDeclaration?) {
        p.printWithNoIndent(expression.symbol.owner.name.asString())
    }

    override fun visitSetValue(expression: IrSetValue, data: IrDeclaration?) {
        p.printWithNoIndent(expression.symbol.owner.name.asString() + " = ")
        expression.value.accept(this, data)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: IrDeclaration?) {
        // TODO what if symbol is unbound?
        expression.symbol.defaultType.printTypeWithNoIndent()
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: IrDeclaration?) {
        val enumEntry = expression.symbol.owner
        p.printWithNoIndent(enumEntry.parentAsClass.name.asString())
        p.printWithNoIndent(".")
        p.printWithNoIndent(enumEntry.name.asString())
    }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference, data: IrDeclaration?) {
        // TODO support
        // TODO no test
        // it's not valid kotlin
        p.printWithNoIndent("&")
        super.visitRawFunctionReference(expression, data)
    }

    override fun visitReturn(expression: IrReturn, data: IrDeclaration?) {
        // TODO label
        // TODO optionally don't print Unit when return type of returnTargetSymbol is Unit
        p.printWithNoIndent("return ")
        expression.value.accept(this, data)
    }

    override fun visitThrow(expression: IrThrow, data: IrDeclaration?) {
        p.printWithNoIndent("throw ")
        expression.value.accept(this, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrDeclaration?) {
        // TODO use triple quotes when possible?
        // TODO optionally each argument at a separate line, another option add a wrapping
        expression.arguments.forEachIndexed { i, e ->
            p(i > 0, " +")
            e.accept(this, data)
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: IrDeclaration?) {
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

    override fun visitVararg(expression: IrVararg, data: IrDeclaration?) {
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

    override fun visitSpreadElement(spread: IrSpreadElement, data: IrDeclaration?) {
        p.printWithNoIndent("*")
        spread.expression.accept(this, data)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclaration?) {
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

    override fun visitWhen(expression: IrWhen, data: IrDeclaration?) {
        // TODO print if when possible?
        p.printlnWithNoIndent("when {")
        p.pushIndent()

        expression.branches.forEach { it.accept(this, data) }

        p.popIndent()
        p.print("}")
    }

    override fun visitBranch(branch: IrBranch, data: IrDeclaration?) {
        p.printIndent()
        branch.condition.accept(this, data)
        p.printWithNoIndent(" -> ")
        branch.result.accept(this, data)
        p.println()
    }

    override fun visitElseBranch(branch: IrElseBranch, data: IrDeclaration?) {
        p.printIndent()
        if ((branch.condition as? IrConst<*>)?.value == true) {
            p.printWithNoIndent("else")
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

    override fun visitWhileLoop(loop: IrWhileLoop, data: IrDeclaration?) {
        loop.printLabel()

        p.printWithNoIndent("while (")
        loop.condition.accept(this, data)
        p.printWithNoIndent(") ")

        loop.body?.accept(this, data)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: IrDeclaration?) {
        loop.printLabel()

        p.printWithNoIndent("do")

        loop.body?.accept(this, data)

        p.print("while (")
        loop.condition.accept(this, data)
        p.printWithNoIndent(")")

    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: IrDeclaration?) {
        // TODO render loop reference
        p.printWithNoIndent(if (jump is IrContinue) "continue" else "break")
        jump.label?.let {
            p.printWithNoIndent("@")
            p.printWithNoIndent(it)
        }
    }

    override fun visitTry(aTry: IrTry, data: IrDeclaration?) {
        p.printWithNoIndent("try ")
        aTry.tryResult.accept(this, data)
        p.printlnWithNoIndent()

        aTry.catches.forEach { it.accept(this, data) }

        aTry.finallyExpression?.let {
            p.print("finally ")
            it.accept(this, data)
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: IrDeclaration?) {
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

    override fun visitGetClass(expression: IrGetClass, data: IrDeclaration?) {
        expression.argument.accept(this, data)
        p.printWithNoIndent("::class")
    }

    override fun visitClassReference(expression: IrClassReference, data: IrDeclaration?) {
        // TODO use classType
        p.printWithNoIndent((expression.symbol.owner as IrDeclarationWithName).name.asString())
        p.printWithNoIndent("::class")
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclaration?) {
        // TODO reflectionTarget
        expression.printCallableReferenceWithNoIndent(expression.symbol.owner.valueParameters, data)
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: IrDeclaration?) {
        // TODO do we need additional fields (field, getter, setter)?
        expression.printCallableReferenceWithNoIndent(emptyList(), data)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference, data: IrDeclaration?) {
        // TODO do we need additional fields (delegate, getter, setter)?
        expression.printCallableReferenceWithNoIndent(emptyList(), data)
    }

    private fun IrCallableReference<*>.printCallableReferenceWithNoIndent(valueParameters: List<IrValueParameter>, data: IrDeclaration?) {
        // TODO where from to get type arguments for a class?
        // TODO rendering for references to constructors
        if (dispatchReceiver == null && extensionReceiver == null) {
            (symbol.owner as IrDeclaration).parentClassOrNull?.let {
                p.printWithNoIndent(it.name.asString())
            }
        }

        printMemberAccessExpressionWithNoIndent(
            referencedName.asString(), // effectively it's same as `symbol.owner.name.asString()`
            valueParameters,
            superQualifierSymbol = null,
            omitAllBracketsIfNoArguments = true,
            data = data,
            accessOperator = "::",
            omitAccessOperatorIfNoReceivers = false,
            wrapArguments = true
        )
    }

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: IrDeclaration?) {
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

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression, data: IrDeclaration?) {
        expression.receiver.accept(this, data)
        p.printWithNoIndent(".")
        p.printWithNoIndent(expression.memberName)
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: IrDeclaration?) {
        // TODO declaration.printlnAnnotations()
        p.println("/* ErrorDeclaration */")
    }

    override fun visitErrorExpression(expression: IrErrorExpression, data: IrDeclaration?) {
        // TODO description
        p.printWithNoIndent("error(\"\") /* ErrorExpression */")
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: IrDeclaration?) {
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

    override fun visitSuspendableExpression(expression: IrSuspendableExpression, data: IrDeclaration?) {
        // TODO support
        // TODO no test
        super.visitSuspendableExpression(expression, data)
    }

    override fun visitSuspensionPoint(expression: IrSuspensionPoint, data: IrDeclaration?) {
        // TODO support
        // TODO no test
        super.visitSuspensionPoint(expression, data)
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
        private const val INAPPLICABLE = false
        private val INAPPLICABLE_N = null
        private const val CUSTOM_MODIFIER_START = "/* "
        private const val CUSTOM_MODIFIER_END = " */"
    }
}
