/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import org.jetbrains.kotlin.decompiler.util.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.Printer

fun IrElement.decompile(): String =
    StringBuilder().also { sb ->
        accept(DecompileIrTreeVisitor(sb), "")
    }.toString().trimEnd()


class DecompileIrTreeVisitor(
    out: Appendable
) : IrElementVisitor<Unit, String> {

    internal val printer = Printer(out, "    ")


    override fun visitBlockBody(body: IrBlockBody, data: String) {
        withBracesLn {
            body.statements
                //Вызовы родительского конструктора, в каких случаях явно оставлять?
                .filterNot { it is IrDelegatingConstructorCall }
                .filterNot { it is IrInstanceInitializerCall }
                .decompileElements()
        }
    }


    override fun visitReturn(expression: IrReturn, data: String) {
        printer.print("$RETURN_TOKEN ")
        expression.value.accept(this, "return")
    }


    override fun visitClass(declaration: IrClass, data: String) {
        printer.print(declaration.obtainDeclarationStr())
        when (declaration.kind) {
            ClassKind.CLASS -> {
                declaration.primaryConstructor?.obtainPrimaryCtor()
                withBracesLn {
                    declaration.declarations
                        .filterIsInstance<IrProperty>()
                        .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                        .decompileElements()

                    val secondaryCtors = declaration.constructors.filterNot { it.isPrimary }
                    secondaryCtors.forEach {
                        it.obtainSecondaryCtor()
                    }
                    declaration.declarations
                        .filterNot { it is IrConstructor || it is IrProperty }
                        .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                        .decompileElements()
                }
            }
            ClassKind.ANNOTATION_CLASS -> {
                withBracesLn {
                    declaration.declarations
                        .filterNot { it is IrConstructor }
                        .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                        .decompileElements()
                }
            }
            else -> {
                val implStr = declaration.obtainInheritance()
                if (implStr.isNotEmpty()) {
                    printer.printWithNoIndent(", $implStr")
                }
                withBracesLn {
                    declaration.declarations
                        .filterNot { it is IrConstructor }
                        .filterNot { it.origin == IrDeclarationOrigin.FAKE_OVERRIDE }
                        .decompileElements()
                }
            }
        }
    }

    private fun IrConstructor.obtainPrimaryCtor() {
        printer.printWithNoIndent(obtainValueParameterTypes())
        val delegatingCtorCall = body!!.statements.filterIsInstance<IrDelegatingConstructorCall>().firstOrNull()
        val implStr = parentAsClass.obtainInheritance()
        val delegatingCtorCallStr = delegatingCtorCall?.decompile() ?: ""
        if (delegatingCtorCallStr.isEmpty()) {
            printer.printWithNoIndent(": $implStr".takeIf { implStr.isNotEmpty() }.orEmpty())
        } else {
            printer.printWithNoIndent("$delegatingCtorCallStr${", $implStr".takeIf { implStr.isNotEmpty() }.orEmpty()}")
        }
    }

    private fun IrConstructor.obtainSecondaryCtor() {
        printer.print("${obtainVisibility()}constructor${obtainValueParameterTypes()}")
        val delegatingCtorCall = body!!.statements.filterIsInstance<IrDelegatingConstructorCall>().first()
        val delegatingCtorClass = delegatingCtorCall.symbol.owner.returnType
        printer.printWithNoIndent(" : ${delegatingCtorCall.renderDelegatingIntoSecondary(returnType != delegatingCtorClass)}")
        val implStr = parentAsClass.obtainInheritance()
        printer.printWithNoIndent(", $implStr".takeIf { implStr.isNotEmpty() }.orEmpty())
        body?.accept(this@DecompileIrTreeVisitor, "")
    }

    private fun IrDelegatingConstructorCall.renderDelegatingIntoSecondary(isSuper: Boolean): String {
        var result = if (isSuper) SUPER_TOKEN else THIS_TOKEN
        result += (0 until valueArgumentsCount).map { getValueArgument(it)?.decompile() }
            .joinToString(", ", "(", ")")
        return result
    }


    // TODO TypeParameters (дженерики)
    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) {
        with(declaration) {
            printer.println(
                ArrayList<String?>().apply {
                    add(obtainTypeAliasFlags())
                    add(obtainVisibility())
                    add(TYPEALIAS_TOKEN)
                    add(name())
                    add(EQ_TOKEN)
                    add(expandedType.toKotlinType().toString())
                }.filterNot { it.isNullOrEmpty() }.joinToString(" ")
            )
        }
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: String) {
        body.expression.accept(this, "")
    }


    //TODO А когда тут DEFAULT_PROPERTY_ACCESSOR и что с ним делать?
    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        with(declaration) {
            if (origin != DEFAULT_PROPERTY_ACCESSOR) {
                printer.print(
                    ArrayList<String?>().apply {
                        add(obtainSimpleFunctionFlags())
                        add(OVERRIDE_TOKEN.takeIf { isOverriden() })
                        add(obtainModality().takeIf { !isOverriden() })
                        add(obtainVisibility())
                        add(FUN_TOKEN)
                        add(obtainFunctionName()
                                    + obtainTypeParameters()
                                    + obtainValueParameterTypes()
                                    + returnType.toKotlinType().takeIf { !it.isUnit() }.let { ": ${returnType.toKotlinType()} " })
                    }.filterNot { it.isNullOrEmpty() }
                        .joinToString(separator = " ")
                )
                declaration.body?.accept(this@DecompileIrTreeVisitor, "")
                printer.printlnWithNoIndent()
            } else {
                printer.printWithNoIndent(declaration.correspondingPropertySymbol!!.owner.name())
            }

        }
    }

    override fun visitVariable(declaration: IrVariable, data: String) {
        var result = run {
            with(declaration) {
                when {
                    origin == IrDeclarationOrigin.CATCH_PARAMETER -> "${name()}: ${type.toKotlinType()}"
                    initializer is IrBlock -> {
                        val variableDeclarations = (initializer as IrBlock).statements.filterIsInstance<IrVariable>()
                        variableDeclarations.forEach { it.accept(this@DecompileIrTreeVisitor, "") }
                        val lastStatement = (initializer as IrBlock).statements.last()
                        "${obtainVariableFlags()} ${name()}: ${type.toKotlinType()} = ${lastStatement.decompile()}"
                    }
                    else -> "${obtainVariableFlags()} ${name()}: ${type.toKotlinType()} = ${initializer?.decompile()}"
                }
            }
        }
        printer.println(result)
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        with(declaration) {
            if (!parentAsClass.isPrimaryCtorArg(name())) {
                var result = ""
                result += ArrayList<String?>().apply {
                    add(obtainVisibility())
                    add(obtainModality())
                    add(obtainPropertyFlags())
                    if (backingField != null) {
                        add("${backingField!!.name()}: ${backingField!!.type.toKotlinType()}")
                        if (backingField!!.initializer != null) {
                            add(EQ_TOKEN)
                            add(backingField!!.initializer!!.decompile())
                        }
                    } else {
                        add("${name()}: ${getter?.returnType?.toKotlinType()}")
                    }
                }.filterNot { it.isNullOrEmpty() }
                    .joinToString(separator = " ")
                printer.println(result)
                getter?.obtainCustomGetter()
                setter?.obtainCustomSetter()
            }
        }
    }

    override fun visitField(declaration: IrField, data: String) {
        var result = "${declaration.name()}: ${declaration.type.toKotlinType()} "
        if (declaration.initializer != null) {
            result += "= ${declaration.initializer!!.decompile()}"
        }
        printer.println(result)
    }

    private fun List<IrElement>.decompileElements() {
        forEach { it.accept(this@DecompileIrTreeVisitor, "") }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: String) {
        var result = ""
        if (!expression.symbol.owner.returnType.isAny()) {
            result += " : ${expression.symbol.owner.returnType.toKotlinType()}"
            result += ((0 until expression.valueArgumentsCount)
                .map { expression.getValueArgument(it)?.decompile() }
                .joinToString(", ", "(", ")"))
        }
        printer.println(result)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: String) {
        printer.println(expression.decompile())
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: String) {
        printer.print(INIT_TOKEN)
        declaration.body.accept(this, "")
    }

    override fun visitWhen(expression: IrWhen, data: String) {
        if (expression is IrIfThenElseImpl) {
            when (expression.origin) {
                IF -> {
                    if (data == "return") {
                        printer.printWithNoIndent(IF_TOKEN)
                    } else {
                        printer.print(IF_TOKEN)
                    }
                    printer.printWithNoIndent(" (${concatenateConditions(expression.branches[0].condition)})")
                    withBracesLn {
                        expression.branches[0].result.accept(this, "")
                    }
                    if (expression.branches.size == 2) {
                        printer.print(ELSE_TOKEN)
                        withBracesLn {
                            expression.branches[1].result.accept(this, "")
                        }
                    }
                }
                else -> TODO()
            }
        } else {
            if (data == "return") {
                printer.printWithNoIndent(WHEN_TOKEN)
            } else {
                printer.print(WHEN_TOKEN)
            }
            withBracesLn {
                expression.branches.decompileElements()
            }
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        if (branch.condition is IrIfThenElseImpl) {
            printer.print(collectCommaArguments(branch.condition))
        } else {
            printer.print(branch.condition.decompile())
        }
        printer.printWithNoIndent(" ->")
        withBracesLn {
            branch.result.accept(this@DecompileIrTreeVisitor, "")
        }
    }

    private fun collectCommaArguments(condition: IrExpression): String = TODO()

    override fun visitElseBranch(branch: IrElseBranch, data: String) {
        printer.print("$ELSE_TOKEN ->")
        withBracesLn {
            branch.result.accept(this, "")
        }

    }

    override fun visitSetVariable(expression: IrSetVariable, data: String) {
        var result = expression.symbol.owner.name()
        result += when (expression.origin) {
            PLUSEQ -> " += "
            MINUSEQ -> " -= "
            MULTEQ -> " *= "
            DIVEQ -> " /= "
            else -> " = "
        }
        result += expression.value.decompile()
        printer.println(result)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: String) {
        printer.println(expression.arguments.joinToString("", "\"", "\"") {
            when (it) {
                is IrGetValue -> "$" + "{" + it.symbol.owner.name() + "}"
                is IrConst<*> -> it.value?.toString().orEmpty()
                else -> it.decompile()
            }
        })
    }


    override fun visitCall(expression: IrCall, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.obtainCall())
        } else {
            printer.println(expression.obtainCall())
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.obtainConstValue())
        } else {
            printer.println(expression.obtainConstValue())
        }
    }

    private fun <T> IrConst<T>.obtainConstValue(): String =
        when (kind) {
            IrConstKind.String -> "\"${value as String}\""
            IrConstKind.Char -> "\'${value as String}\'"
            IrConstKind.Null -> "null"
            else -> value.toString()
        }


    override fun visitGetValue(expression: IrGetValue, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.obtainGetValue())
        } else {
            printer.printWithNoIndent(expression.obtainGetValue())
        }
    }

    private fun IrGetValue.obtainGetValue(): String {
        return when {
            origin == INITIALIZE_PROPERTY_FROM_PARAMETER -> EMPTY_TOKEN
            symbol.owner.name() == "<this>" || symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER -> THIS_TOKEN
            else -> symbol.owner.name()
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: String) {
        var result = expression.type.toKotlinType().toString()
        var irConstructor = expression.symbol.owner
        //TODO добавить проверку наличия defaultValue и именнованных вызовов
        result += ((0 until expression.valueArgumentsCount).map { expression.getValueArgument(it)?.decompile() }
            .filterNotNull()
            .joinToString(", ", "(", ")"))
        printer.printWithNoIndent(result)
    }

    override fun visitGetField(expression: IrGetField, data: String) {
        TODO()
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        val initValue = expression.value.decompile()
        val receiverValue = expression.receiver?.decompile() ?: ""
        val backFieldSymbolVal = expression.symbol.owner.name()
        printer.println("$receiverValue.$backFieldSymbolVal = $initValue")
    }


    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        printer.print("$WHILE_TOKEN (${loop.condition.decompile()})")
        withBracesLn {
            loop.body?.accept(this, "")
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        printer.print(DO_TOKEN)
        withBraces {
            loop.body?.accept(this@DecompileIrTreeVisitor, "")
        }
        printer.printlnWithNoIndent("$WHILE_TOKEN (${loop.condition.decompile()})")
    }

    override fun visitThrow(expression: IrThrow, data: String) {
        printer.println("$THROW_TOKEN ${expression.value.decompile()}")
    }


    override fun visitTry(aTry: IrTry, data: String) {
        if (data == RETURN_TOKEN) {
            printer.printWithNoIndent(TRY_TOKEN)
        } else {
            printer.print(TRY_TOKEN)
        }
        withBracesLn {
            aTry.tryResult.accept(this, "")
        }
        aTry.catches.decompileElements()
        if (aTry.finallyExpression != null) {
            printer.print("$FINALLY_TOKEN ")
            withBracesLn {
                aTry.finallyExpression!!.accept(this, "")
            }
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: String) {
        printer.print("$CATCH_TOKEN (${aCatch.catchParameter.decompile()}) ")
        withBracesLn {
            aCatch.result.accept(this, "")
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
        with(expression) {
            when (operator) {
                IMPLICIT_COERCION_TO_UNIT -> printer.println(argument.decompile())
                INSTANCEOF -> printer.print("(${argument.decompile()} is ${typeOperand.toKotlinType()})")
                NOT_INSTANCEOF -> printer.print("${argument.decompile()} !is ${typeOperand.toKotlinType()}")
                else -> TODO("Unexpected type operator $operator!")
            }

        }
    }

    override fun visitBody(body: IrBody, data: String) {
        body.acceptChildren(this, data)
    }

    override fun visitBlock(expression: IrBlock, data: String) {
        expression.acceptChildren(this, data)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: String) {
        declaration.acceptChildren(this, data)
    }

    override fun visitFile(declaration: IrFile, data: String) {
        declaration.acceptChildren(this, data)
    }

    override fun visitComposite(expression: IrComposite, data: String) {
        expression.acceptChildren(this, "")
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) = TODO()

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) = TODO()
    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) = TODO()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) = TODO()

    //TODO если его заменяют obtainPrimaryCtor и obtainSecondaryCtor - выпилить
    override fun visitConstructor(declaration: IrConstructor, data: String) = TODO()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) = TODO()

    override fun visitElement(element: IrElement, data: String) = TODO()

}

