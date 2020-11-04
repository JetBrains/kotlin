/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer
// TODO look at
//  IrSourcePrinter.kt -- androidx-master-dev/frameworks/support/compose/compiler/compiler-hosted/src/main/java/androidx/compose/compiler/plugins/kotlin/lower/IrSourcePrinter.kt
//  DumpIrTree.kt -- compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/util/DumpIrTree.kt
//  RenderIrElement.kt --

/* TODO
    * origin : class, function, property, ...
    * don't print any members in interfaces? // or just print something like  /* Any members */
    * @<init>(...) Function2<AStringUnit> // (...) -> ...
    * IrEnumEntryImpl
    * replace `if (cond) p.printWithNoIndent(something)` with `p(cond, something)`
    * use FQNs?
    * don't print coercion to Unit on top level? blocks?
    * name & ordinal in enums
    * special render for accessors?
    * FlexibleNullability
    * ExtensionFunctionType
    * don't crash on unbound symbols
    * unique ids for symbols, or SignatureID?
    * "normalize" names for tmps?
 */

fun IrElement.dumpKotlinLike(options: KotlinLikeDumpOptions = KotlinLikeDumpOptions()): String {
    val sb = StringBuilder()
    acceptVoid(KotlinLikeDumper(Printer(sb, "  "), options))
    return sb.toString()
}

class KotlinLikeDumpOptions(
    val printRegionsPerFile: Boolean = false,
    val printFileName: Boolean = true,
    val printFilePath: Boolean = true,
    val useNamedArguments: Boolean = false,
    val labelStrategy: LabelPrintingStrategy = LabelPrintingStrategy.NEVER,
    val printFakeOverridesStrategy: FakeOverridesStrategy = FakeOverridesStrategy.ALL,
    /*
     visibility?
     modality
     special names?
     print fake overrides
     omit local visibility?
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

private class KotlinLikeDumper(val p: Printer, val options: KotlinLikeDumpOptions) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        val e = "/* ERROR: unsupported element type: " + element.javaClass.simpleName + " */"
        if (element is IrExpression) {
            // TODO message?
            // TODO better process expressions and statements
            p.printlnWithNoIndent("error(\"\") $e")
        } else {
            p.println(e)
        }
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        declaration.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        if (options.printRegionsPerFile) p.println("//region block: ${declaration.name}")

        if (options.printFileName) p.println("// FILE: ${declaration.name}")
        if (options.printFilePath) p.println("// path: ${declaration.path}")
        declaration.printlnAnnotations("file")
        val packageFqName = declaration.packageFragmentDescriptor.fqName
        if (!packageFqName.isRoot) {
            p.println("package ${packageFqName.asString()}")
        }
        if (!p.isEmpty) p.printlnWithNoIndent()

        declaration.declarations.forEach { it.acceptVoid(this) }

        if (options.printRegionsPerFile) p.println("//endregion")
    }

    override fun visitClass(declaration: IrClass) {
        // TODO omit super class for enums, annotations?
        // TODO omit Companion name for companion objects?
        // TODO thisReceiver
        // TODO primary constructor?

        declaration.printlnAnnotations()
        p.print("")

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

        declaration.declarations.forEach { it.acceptVoid(this) }

        p.popIndent()
        p.println("}")
        p.printlnWithNoIndent()
    }

    private fun IrFunction.printValueParametersWithNoIndent() {
        p.printWithNoIndent("(")
        var first = true
        valueParameters.forEach {
            if (!first) {
                p.printWithNoIndent(", ")
            } else {
                first = false
            }

            printParameterModifiersWithNoIndent(
                isVararg = it.varargElementType != null,
                it.isCrossinline,
                it.isNoinline,
            )

            p.printWithNoIndent(it.name.asString())
            p.printWithNoIndent(": ")
            (it.varargElementType ?: it.type).printTypeWithNoIndent()
            // TODO print it.type too for varargs?

            it.defaultValue?.let { v ->
                p.printWithNoIndent(" = ")
                v.acceptVoid(this@KotlinLikeDumper)
            }
        }
        p.printWithNoIndent(")")
    }

    private fun IrTypeParametersContainer.printWhereClauseIfNeededWithNoIndent() {
        if (typeParameters.none { it.superTypes.size > 1 }) return

        p.printWithNoIndent(" where ")

        var first = true
        typeParameters.forEach {
            if (it.superTypes.size > 1) {
                it.superTypes.forEach { superType ->
                    if (!first) {
                        p.printWithNoIndent(", ")
                    } else {
                        first = false
                    }

                    p.printWithNoIndent(it.name.asString())
                    p.printWithNoIndent(" : ")
                    superType.printTypeWithNoIndent()
                }
            }
        }
    }

    private fun IrTypeParametersContainer.printTypeParametersWithNoIndent(postfix: String = "") {
        if (typeParameters.isEmpty()) return

        p.printWithNoIndent("<")
        var first = true
        // TODO no commas in some types
        typeParameters.forEach {
            if (!first) {
                p.printWithNoIndent(", ")
            } else {
                first = false
            }

            it.variance.printVarianceWithNoIndent()
            if (it.isReified) p.printWithNoIndent("reified ")

            it.printAnnotationsWithNoIndent()

            p.printWithNoIndent(it.name.asString())

            if (it.superTypes.size == 1) {
                p.printWithNoIndent(" : ")
                it.superTypes.single().printTypeWithNoIndent()
            }
        }
        p.printWithNoIndent(">")
        p.printWithNoIndent(postfix)
    }

    private fun Variance.printVarianceWithNoIndent() {
        if (this != Variance.INVARIANT) {
            p.printWithNoIndent("$label ")
        }
    }

    private fun IrConstructorCall.printAnAnnotationWithNoIndent(prefix: String = "") {
        val clazz = symbol.owner.parentAsClass
        assert(clazz.isAnnotationClass)
        // TODO render arguments / reuse visitCall or visitConstructorCall
        p.printWithNoIndent("@" + (if (prefix.isEmpty()) "" else "$prefix:") + clazz.name.asString())
        if (valueArgumentsCount > 0) p.printWithNoIndent("(...)")
    }

    private fun IrAnnotationContainer.printlnAnnotations(prefix: String = "") {
        annotations.forEach {
            p.print("")
            it.printAnAnnotationWithNoIndent(prefix)
            p.printlnWithNoIndent()
        }
    }

    private fun IrAnnotationContainer.printAnnotationsWithNoIndent() {
        annotations.forEach {
            it.printAnAnnotationWithNoIndent()
            p.printWithNoIndent(" ")
        }
    }

    private fun IrType.printTypeWithNoIndent() {
        // TODO don't print `Any?` upper bound?
        printAnnotationsWithNoIndent()
        when (this) {
            is IrSimpleType -> {
                // TODO abbreviation

                p.printWithNoIndent((classifier.owner as IrDeclarationWithName).name.asString())

                if (arguments.isNotEmpty()) {
                    p.printWithNoIndent("<")
                    var first = true
                    arguments.forEach {
                        if (!first) {
                            p.printWithNoIndent(", ")
                        } else {
                            first = false
                        }

                        when(it) {
                            is IrStarProjection ->
                                p.printWithNoIndent("*")
                            is IrTypeProjection -> {
                                it.variance.printVarianceWithNoIndent()
                                it.type.printTypeWithNoIndent()
                            }
                        }
                    }
                    p.printWithNoIndent(">")
                }

                if (hasQuestionMark) p.printWithNoIndent("?")
            }
            is IrDynamicType ->
                p.printWithNoIndent("dynamic")
            is IrErrorType ->
                p.printWithNoIndent("ErrorType /* ERROR */")
            else ->
                p.printWithNoIndent("??? /* ERROR: unknown type: ${this.javaClass.simpleName} */")
        }
    }

    private fun p(condition: Boolean, s: String) {
        if (condition) p.printWithNoIndent("$s ")
    }

    private fun <T : Any> p(value: T?, defaultValue: T? = null, getString: T.() -> String) {
        if (value == null) return
        p(value != defaultValue, value.getString())
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
        visibility: Visibility,
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
        p(isFakeOverride, "fake") // TODO
        p(isOverride, "override")
        p(isLateinit, "lateinit")
        p(isTailrec, "tailrec")
        printParameterModifiersWithNoIndent(
            isVararg,
            isCrossinline = INAPPLICABLE,
            isNoinline = INAPPLICABLE
        )
        p(isSuspend, "suspend")
        p(isInner, "inner")
        p(isInline, "inline")
        p(isData, "data")
        p(isCompanion, "companion")
        p(isFunInterface, "fun")
        p(classKind) { name.toLowerCase().replace('_', ' ') }
        p(isInfix, "infix")
        p(isOperator, "operator")
    }

    private fun printVisibility(visibility: Visibility) {
        // TODO don't print visibility if it's not changed in override?
        p(visibility, Visibilities.DEFAULT_VISIBILITY) { name }
    }

    private fun printParameterModifiersWithNoIndent(
        isVararg: Boolean,
        isCrossinline: Boolean,
        isNoinline: Boolean,
    ) {
        p(isVararg, "vararg")
        p(isCrossinline, "crossinline")
        p(isNoinline, "noinline")
    }

    private val INAPPLICABLE = false
    private val INAPPLICABLE_N = null

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.printSimpleFunction(
            "fun ",
            declaration.name.asString(),
            printTypeParametersAndExtensionReceiver = true,
            printSignatureAndBody = true
        )
        p.printlnWithNoIndent()
    }

    private fun IrValueParameter.printExtensionReceiverParameter() {
        type.printTypeWithNoIndent()
        p.printWithNoIndent(".")
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
                isOverride = overriddenSymbols.isNotEmpty(), // TODO override
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

            body?.acceptVoid(this@KotlinLikeDumper)
        } else {
            p.printlnWithNoIndent()
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        // TODO primary!!!
        // TODO name?
        // TODO is it worth to merge code for IrConstructor and IrSimpleFunction?
        // TODO dispatchReceiverParameter -- outer `this` for inner classes
        // TODO return type?

        declaration.printlnAnnotations()
        p.print("")

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
        p(declaration.isPrimary, commentBlockH("primary"))
        declaration.body?.acceptVoid(this)
        p.printlnWithNoIndent()
    }

    private fun commentBlock(text: String) = "/* $text */"
    private fun commentBlockH(text: String) = "/* $text */"

    override fun visitProperty(declaration: IrProperty) {
        if (options.printFakeOverridesStrategy == FakeOverridesStrategy.NONE && declaration.isFakeOverride) return

        declaration.printlnAnnotations()
        p.print("")

        // TODO better rendering for modifiers on property and accessors
        // modifiers that could be different between accessors and property have a comment
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

        // TODO don't print borrowed flags and assert that they are same with setter(???) or don't borrow?
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
        // TODO it's not valid kotlin
        declaration.backingField?.initializer?.let {
            p.print("field = ")
            it.acceptVoid(this)
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

    override fun visitField(declaration: IrField) {
        declaration.printlnAnnotations()
        p.print("")

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
            p.printWithNoIndent("/*")
            p(declaration.isStatic, "static")
            p(declaration.isFinal, "final")
            p.printWithNoIndent("field*/ ")
        }

        p.printWithNoIndent(if (declaration.isFinal) "val " else "var ")
        p.printWithNoIndent(declaration.name.asString() + ": ")
        declaration.type.printTypeWithNoIndent()

        declaration.initializer?.let {
            p.printWithNoIndent(" = ")
            it.acceptVoid(this)
        }

        // TODO correspondingPropertySymbol

        p.printlnWithNoIndent()
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        declaration.printlnAnnotations()
        p.print("")

        printVisibility(declaration.visibility)
        p(declaration.isActual, "actual")

        p.printWithNoIndent("typealias ")
        p.printWithNoIndent(declaration.name.asString())
        // TODO IrDump doesn't have typeparameters
        declaration.printTypeParametersWithNoIndent()
        p.printWithNoIndent(" = ")
        declaration.expandedType.printTypeWithNoIndent()

        p.printlnWithNoIndent()
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.printlnAnnotations()
        p.print("")

        p(declaration.isLateinit, "lateinit")
        p(declaration.isConst, "const")
        declaration.run { printVariable(isVar, name, type) }

        declaration.initializer?.let {
            p.printWithNoIndent(" = ")
            it.acceptVoid(this)
        }
    }

    private fun printVariable(isVar: Boolean, name: Name, type: IrType) {
        p.printWithNoIndent(if (isVar) "var" else "val")
        p.printWithNoIndent(" ")
        p.printWithNoIndent(name.asString())
        p.printWithNoIndent(": ")
        type.printTypeWithNoIndent()
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        declaration.printlnAnnotations()
        p.print("")

        // TODO correspondingClass
        p.printWithNoIndent(declaration.name)
        declaration.initializerExpression?.let {
            // TODO better rendering for init
            p.printWithNoIndent(" init = ")
            it.acceptVoid(this)
        } ?: p.printlnWithNoIndent()
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        // Looks like IrAnonymousInitializer has annotations accidentally. No tests.
        declaration.printlnAnnotations()
        p.print("")

        // TODO looks like there are no irText tests for isStatic flag
        p(declaration.isStatic, commentBlockH("static"))
        p.printWithNoIndent("init ")
        declaration.body.acceptVoid(this)

        p.printlnWithNoIndent()
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) {
        declaration.printlnAnnotations()
        p.print("")

        // TODO think about better rendering
        declaration.run { printVariable(isVar, name, type) }

        p.printlnWithNoIndent()
        p.pushIndent()

        declaration.delegate.acceptVoid(this)
        p.printlnWithNoIndent()

        declaration.getter.printAccessor("get")
        declaration.setter?.printAccessor("set")

        p.popIndent()
        p.printlnWithNoIndent()
    }

    override fun visitExpressionBody(body: IrExpressionBody) {
        // TODO should we print something here?
        body.expression.acceptVoid(this)
    }

    override fun visitBlockBody(body: IrBlockBody) {
        body.printStatementContainer("{", "}")
        p.printlnWithNoIndent()
    }

    private fun IrStatementContainer.printStatementContainer(before: String, after: String, withIndentation: Boolean = true) {
        p.printlnWithNoIndent(before)
        if (withIndentation) p.pushIndent()

        statements.forEach {
            if (it is IrExpression) p.printIndent()
            it.acceptVoid(this@KotlinLikeDumper)
            p.printlnWithNoIndent()
        }

        if (withIndentation) p.popIndent()
        p.print(after)
    }

    override fun visitSyntheticBody(body: IrSyntheticBody) {
        p.printlnWithNoIndent("/* Synthetic body for ${body.kind} */")
    }

    override fun visitCall(expression: IrCall) {
        val declaration = expression.symbol.owner

        expression.dispatchReceiver?.let {
            it.acceptVoid(this)
            p.printWithNoIndent(".")
        }

        p.printWithNoIndent(declaration.name.asString())

        if (expression.typeArgumentsCount > 0) {
            p.printWithNoIndent("<")
            repeat(expression.typeArgumentsCount) {
                if (it > 0) {
                    p.printWithNoIndent(", ")
                }
                expression.getTypeArgument(it)!!.printTypeWithNoIndent()
            }
            p.printWithNoIndent(">")
        }

        p.printWithNoIndent("(")
        expression.extensionReceiver?.let {
            // TODO
            p.printWithNoIndent("\$receiver = ")
            it.acceptVoid(this)
            if (expression.valueArgumentsCount > 0) p.printWithNoIndent(", ")
        }

        repeat(expression.valueArgumentsCount) { i ->
            expression.getValueArgument(i)?.let {
                if (i > 0) p.printWithNoIndent(", ")
                p.printWithNoIndent(declaration.valueParameters[i].name.asString() + " = ")
                it.acceptVoid(this)
            }
        }

        p.printWithNoIndent(")")
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        // TODO
        p.printWithNoIndent("TODO(\"IrConstructorCall\")")
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        // TODO
        p.printWithNoIndent("TODO(\"IrDelegatingConstructorCall\")")
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        // TODO
        p.printWithNoIndent("TODO(\"IrEnumConstructorCall\")")
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        // TODO support
        // TODO omit the name when it's possible
        // TODO Is there a difference between `<anonymous>` and `<no name provided>`?
        // TODO Is name of function used somehere? How it's important?
        // TODO Use lambda syntax when possible
        // TODO don't print visibility?
        // TODO don't insert indentations, including when there are annotations
        expression.function.printSimpleFunction("fun ", expression.function.name.asString(), printTypeParametersAndExtensionReceiver = true, printSignatureAndBody = true)
    }

    private fun IrFieldAccessExpression.printFieldAccess() {
        // TODO receiver, superQualifierSymbol?
        // TODO is not valid kotlin
        p.printWithNoIndent("#" + symbol.owner.name.asString())
    }

    override fun visitGetField(expression: IrGetField) {
        expression.printFieldAccess()
    }

    override fun visitSetField(expression: IrSetField) {
        expression.printFieldAccess()
        p.printWithNoIndent(" = ")
        expression.value.acceptVoid(this)
    }

    override fun visitReturn(expression: IrReturn) {
        p.printWithNoIndent("return ")
        expression.value.acceptVoid(this)
    }

    override fun visitThrow(expression: IrThrow) {
        p.printWithNoIndent("throw ")
        expression.value.acceptVoid(this)
    }

    override fun visitComposite(expression: IrComposite) {
        expression.printStatementContainer("// COMPOSITE {", "// }", withIndentation = false)
    }

    override fun visitBlock(expression: IrBlock) {
        expression.printStatementContainer("{ //BLOCK", "}")
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation) {
        // TODO escape? see IrTextTestCaseGenerated.Expressions#testStringTemplates
        expression.arguments.forEachIndexed { i, e ->
            if (i > 0) {
                p.printlnWithNoIndent(" + ")
            }
            e.acceptVoid(this)
        }
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        val kind = expression.kind

        val (prefix, postfix) = when (kind) {
            is IrConstKind.Null -> "" to ""
            is IrConstKind.Boolean -> "" to ""
            is IrConstKind.Char -> "'" to "'"
            is IrConstKind.Byte -> "" to "B"
            is IrConstKind.Short -> "" to "S"
            is IrConstKind.Int -> "" to ""
            is IrConstKind.Long -> "" to "L"
            is IrConstKind.String -> "\"" to "\""
            is IrConstKind.Float -> "" to "F"
            is IrConstKind.Double -> "" to "D"
        }

        p.printWithNoIndent(prefix, expression.value ?: "null", postfix)
    }

    override fun visitVararg(expression: IrVararg) {
        p.printWithNoIndent("[")
        expression.elements.forEachIndexed { i, e ->
            if (i > 0) p.printWithNoIndent(", ")
            e.acceptVoid(this)
        }
        p.printWithNoIndent("]")
    }

    override fun visitSpreadElement(spread: IrSpreadElement) {
        p.printWithNoIndent("*")
        spread.expression.acceptVoid(this)
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)
    }

    override fun visitSingletonReference(expression: IrGetSingletonValue) {
        // TODO check
        expression.type.printTypeWithNoIndent()
    }

    override fun visitGetValue(expression: IrGetValue) {
        // TODO support `this` and receiver
        p.printWithNoIndent(expression.symbol.owner.name.asString())
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        p.printWithNoIndent(expression.symbol.owner.name.asString() + " = ")
        expression.value.acceptVoid(this)
    }

    override fun visitGetClass(expression: IrGetClass) {
        expression.argument.acceptVoid(this)
        p.printWithNoIndent("::class")
    }

    override fun visitCallableReference(expression: IrCallableReference<*>) {
        // TODO check
        p.printWithNoIndent("::")
        p.printWithNoIndent(expression.referencedName.asString())
    }

    override fun visitClassReference(expression: IrClassReference) {
        // TODO use type
        p.printWithNoIndent((expression.symbol.owner as IrDeclarationWithName).name.asString())
        p.printWithNoIndent("::class")
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        p.println("/* InstanceInitializerCall */")
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
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

        expression.argument.acceptVoid(this)
        p.printWithNoIndent(" $operator ")
        expression.typeOperand.printTypeWithNoIndent()
        p.printWithNoIndent(after)

    }

    override fun visitWhen(expression: IrWhen) {
        p.printlnWithNoIndent("when {")
        p.pushIndent()

        for (b in expression.branches) {
            p.printIndent()
            b.condition.acceptVoid(this)

            p.printWithNoIndent(" -> ")

            b.result.acceptVoid(this)

            p.println()
        }

        p.popIndent()
        p.print("}")
    }

    override fun visitWhileLoop(loop: IrWhileLoop) {
        p.printWithNoIndent("while (")
        loop.condition.acceptVoid(this)

        p.printWithNoIndent(") ")

        loop.body?.acceptVoid(this)
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        p.printWithNoIndent("do")

        loop.body?.acceptVoid(this)

        p.print("while (")
        loop.condition.acceptVoid(this)
        p.printWithNoIndent(")")

    }

    override fun visitTry(aTry: IrTry) {
        p.printWithNoIndent("try ")
        aTry.tryResult.acceptVoid(this)
        p.printlnWithNoIndent()

        aTry.catches.forEach { it.acceptVoid(this) }

        aTry.finallyExpression?.let {
            p.print("finally ")
            it.acceptVoid(this)
        }
    }

    override fun visitCatch(aCatch: IrCatch) {
        p.print("catch (...) ")
        aCatch.result.acceptVoid(this)
        p.printlnWithNoIndent()
    }

    override fun visitBreak(jump: IrBreak) {
        // TODO label
        p.printWithNoIndent("break")
    }

    override fun visitContinue(jump: IrContinue) {
        // TODO label
        p.printWithNoIndent("continue")
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
        p.println("/* ERROR DECLARATION */")
    }

    override fun visitErrorExpression(expression: IrErrorExpression) {
        // TODO description
        p.printWithNoIndent("error(\"\") /* ERROR EXPRESSION */")
    }

    override fun visitErrorCallExpression(expression: IrErrorCallExpression) {
        // TODO receiver, arguments
        p.printWithNoIndent("error(\"\") /* ERROR CALL */")
    }
}
