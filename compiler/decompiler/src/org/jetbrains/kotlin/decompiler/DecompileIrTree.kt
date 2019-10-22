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
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.Printer

fun IrElement.decompile(data: String): String =
    StringBuilder().also { sb ->
        accept(DecompileIrTreeVisitor(sb), data)
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
                .decompileElements(data)
        }
    }


    override fun visitReturn(expression: IrReturn, data: String) {
        if (data != "lambda") {
            printer.print("$RETURN_TOKEN ")
            expression.value.accept(this, "return")
        } else {
            expression.value.accept(this, data)
        }
    }


    override fun visitClass(declaration: IrClass, data: String) {
        printer.print(declaration.obtainDeclarationStr())
        when (declaration.kind) {
            ClassKind.CLASS -> {
                declaration.primaryConstructor?.obtainPrimaryCtor()
                withBracesLn {
                    declaration.declarations
                        .filterIsInstance<IrProperty>()
                        .filterNot {
                            it.origin in setOf(
                                IrDeclarationOrigin.FAKE_OVERRIDE,
                                IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER
                            )
                        }
                        .decompileElements(data)

                    val secondaryCtors = declaration.constructors.filterNot { it.isPrimary }
                    secondaryCtors.forEach {
                        it.obtainSecondaryCtor()
                    }
                    declaration.declarations
                        .filterNot { it is IrConstructor || it is IrProperty || it is IrEnumEntry }
                        .filterNot {
                            it.origin in setOf(
                                IrDeclarationOrigin.FAKE_OVERRIDE,
                                IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER
                            )
                        }
                        .decompileElements(data)
                }
            }
            ClassKind.ANNOTATION_CLASS -> {
                withBracesLn {
                    declaration.declarations
                        .filterNot { it is IrConstructor }
                        .filterNot {
                            it.origin == IrDeclarationOrigin.FAKE_OVERRIDE
                        }
                        .decompileElements(data)
                }
            }
            ClassKind.ENUM_CLASS -> {
                declaration.primaryConstructor?.obtainPrimaryCtor()
                withBracesLn {
                    printer.println(declaration.declarations
                                        .filterIsInstance<IrEnumEntry>()
                                        .joinToString(", ", postfix = ";") { it.decompile(data) }
                    )
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
                        .decompileElements(data)
                }
            }
        }
    }

    private fun IrConstructor.obtainPrimaryCtor() {
        printer.printWithNoIndent(obtainValueParameterTypes())
        val delegatingCtorCall = body!!.statements.filterIsInstance<IrDelegatingConstructorCall>().firstOrNull()
        val implStr = parentAsClass.obtainInheritance()
        val delegatingCtorCallStr = delegatingCtorCall?.decompile("") ?: ""
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
        result += (0 until valueArgumentsCount).map { getValueArgument(it)?.decompile("") }
            .joinToString(", ", "(", ")")
        return result
    }


    //TODO TypeParameters (дженерики)
    override fun visitTypeAlias(declaration: IrTypeAlias, data: String) {
        with(declaration) {
            printer.println(
                concatenateNonEmptyWithSpace(
                    obtainTypeAliasFlags(),
                    obtainVisibility(),
                    TYPEALIAS_TOKEN,
                    name(),
                    OPERATOR_TOKENS[EQ],
                    expandedType.obtainTypeDescription()
                )
            )
        }
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: String) {
        body.expression.accept(this, data)
    }


    //TODO А когда тут DEFAULT_PROPERTY_ACCESSOR и что с ним делать?
    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: String) {
        with(declaration) {
            if (origin != DEFAULT_PROPERTY_ACCESSOR) {
                printer.print(
                    concatenateNonEmptyWithSpace(
                        obtainSimpleFunctionFlags(),
                        OVERRIDE_TOKEN.takeIf { isOverriden() },
                        obtainModality().takeIf { !isOverriden() },
                        obtainVisibility(),
                        FUN_TOKEN,
                        obtainTypeParameters(),
                        "${obtainFunctionName()}${obtainValueParameterTypes()}",
                        if (!returnType.isUnit()) ": ${returnType.obtainTypeDescription()} " else EMPTY_TOKEN
                    )
                )
                declaration.body?.accept(this@DecompileIrTreeVisitor, data)
                printer.printlnWithNoIndent()
            } else {
                printer.printWithNoIndent(declaration.correspondingPropertySymbol!!.owner.name())
            }

        }
    }

    override fun visitVariable(declaration: IrVariable, data: String) {
        printer.println(
            with(declaration) {
                when {
                    origin == IrDeclarationOrigin.CATCH_PARAMETER -> concatenateNonEmptyWithSpace(
                        name(),
                        ":",
                        type.obtainTypeDescription()
                    )
                    initializer is IrBlock -> {
                        val variableDeclarations = (initializer as IrBlock).statements.filterIsInstance<IrVariable>()
                        variableDeclarations.forEach { it.accept(this@DecompileIrTreeVisitor, data) }
                        val lastStatement = (initializer as IrBlock).statements.last()
                        concatenateNonEmptyWithSpace(
                            obtainVariableFlags(),
                            name(),
                            ":",
                            type.obtainTypeDescription(),
                            "=",
                            lastStatement.decompile(data)
                        )
                    }
                    else -> concatenateNonEmptyWithSpace(
                        obtainVariableFlags(),
                        name(),
                        ":",
                        type.obtainTypeDescription(),
                        "=",
                        initializer?.decompile(data)
                    )
                }
            }
        )
    }

    override fun visitProperty(declaration: IrProperty, data: String) {
        with(declaration) {
            if (!parentAsClass.isPrimaryCtorArg(name()) && origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                var result = concatenateNonEmptyWithSpace(obtainVisibility(), obtainModality(), obtainPropertyFlags())
                if (backingField != null) {
                    result = concatenateNonEmptyWithSpace(result, backingField!!.name(), ":", backingField!!.type.obtainTypeDescription())
                    if (backingField!!.initializer != null) {
                        result = concatenateNonEmptyWithSpace(result, "=", backingField!!.initializer!!.decompile(data))
                    }
                } else {
                    result = concatenateNonEmptyWithSpace(result, name(), ":", getter?.returnType?.toKotlinType().toString())
                }
                printer.println(result)
                getter?.obtainCustomGetter()
                setter?.obtainCustomSetter()
            }
        }
    }

    override fun visitField(declaration: IrField, data: String) {
        var result = concatenateNonEmptyWithSpace(declaration.name(), ":", declaration.type.obtainTypeDescription())
        if (declaration.initializer != null) {
            result = concatenateNonEmptyWithSpace(result, "=", declaration.initializer!!.decompile(data))
        }
        printer.println(result)
    }

    private fun List<IrElement>.decompileElements(data: String) {
        forEach { it.accept(this@DecompileIrTreeVisitor, data) }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: String) {
        var result = ""
        if (!expression.symbol.owner.returnType.isAny()) {
            result = concatenateNonEmptyWithSpace(":", expression.symbol.owner.returnType.obtainTypeDescription(),
                                                  ((0 until expression.valueArgumentsCount)
                                                      .map { expression.getValueArgument(it)?.decompile(data) }
                                                      .joinToString(", ", "(", ")")))
        }
        printer.println(result)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: String) {
        printer.println(expression.decompile(data))
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: String) {
        printer.print(INIT_TOKEN)
        declaration.body.accept(this, data)
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
                    printer.printWithNoIndent(" (${expression.branches[0].decompile(data)})")
                    withBracesLn {
                        expression.branches[0].result.accept(this, data)
                    }
                    if (expression.branches.size == 2) {
                        printer.print(ELSE_TOKEN)
                        withBracesLn {
                            expression.branches[1].result.accept(this, data)
                        }
                    }
                }
                OROR -> printer.printWithNoIndent(
                    concatenateNonEmptyWithSpace(
                        expression.branches[0].condition.decompile(data),
                        OPERATOR_TOKENS[expression.origin],
                        expression.branches[1].result.decompile(data)
                    )
                )

                ANDAND -> printer.printWithNoIndent(
                    "(" + concatenateNonEmptyWithSpace(
                        expression.branches[0].condition.decompile(data),
                        OPERATOR_TOKENS[expression.origin],
                        expression.branches[0].result.decompile(data)
                    ) + ")"
                )
                else -> TODO()
            }
        } else {
            if (data == "return") {
                printer.printWithNoIndent(WHEN_TOKEN)
            } else {
                printer.print(WHEN_TOKEN)
            }
            withBracesLn {
                expression.branches.forEach {
                    if (it is IrElseBranch) {
                        printer.print("else ->")
                    } else {
                        printer.print(" (${it.condition.decompile(data)}) ->")
                    }
                    withBracesLn {
                        it.result.accept(this, data)
                    }
                }
            }
        }
    }

    override fun visitBranch(branch: IrBranch, data: String) {
        printer.print(branch.condition.decompile(data))
    }

    private fun collectCommaArguments(condition: IrExpression): String = TODO()

    override fun visitElseBranch(branch: IrElseBranch, data: String) {
        printer.print("$ELSE_TOKEN ->")
        withBracesLn {
            branch.result.accept(this, data)
        }

    }

    override fun visitSetVariable(expression: IrSetVariable, data: String) {
        printer.println(
            concatenateNonEmptyWithSpace(
                expression.symbol.owner.name(),
                when (expression.origin) {
                    PLUSEQ -> " += "
                    MINUSEQ -> " -= "
                    MULTEQ -> " *= "
                    DIVEQ -> " /= "
                    else -> " = "
                },
                expression.value.decompile(data)
            )
        )
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: String) {
        printer.println(expression.arguments.joinToString("", "\"", "\"") {
            when (it) {
                is IrGetValue -> "$" + "{" + it.symbol.owner.name() + "}"
                is IrConst<*> -> it.value?.toString().orEmpty()
                else -> it.decompile(data)
            }
        })
    }


    override fun visitCall(expression: IrCall, data: String) {
        if (data == "return") {
            printer.println(expression.obtainCall())
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
            IrConstKind.String -> "\"${value.toString()}\""
            IrConstKind.Char -> "\'${value.toString()}\'"
            IrConstKind.Null -> "null"
            else -> value.toString()
        }


    override fun visitGetValue(expression: IrGetValue, data: String) {
        if (data == "return") {
            printer.printlnWithNoIndent(expression.obtainGetValue())
        } else {
            printer.println(expression.obtainGetValue())
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
        var result = expression.type.obtainTypeDescription()
        var irConstructor = expression.symbol.owner
        //TODO добавить проверку наличия defaultValue и именнованных вызовов
        result += (0 until expression.valueArgumentsCount)
            .mapNotNull { expression.getValueArgument(it)?.decompile(data) }
            .joinToString(", ", "(", ")")
        printer.println(result)
    }

    override fun visitGetField(expression: IrGetField, data: String) {
        TODO()
    }

    override fun visitSetField(expression: IrSetField, data: String) {
        val initValue = expression.value.decompile(data)
        val receiverValue = expression.receiver?.decompile(data) ?: ""
        val backFieldSymbolVal = expression.symbol.owner.name()
        printer.println("$receiverValue.$backFieldSymbolVal = $initValue")
    }


    override fun visitWhileLoop(loop: IrWhileLoop, data: String) {
        printer.print("$WHILE_TOKEN (${loop.condition.decompile(data)})")
        withBracesLn {
            loop.body?.accept(this, data)
        }
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: String) {
        printer.print(DO_TOKEN)
        withBraces {
            loop.body?.accept(this@DecompileIrTreeVisitor, data)
        }
        printer.printlnWithNoIndent("$WHILE_TOKEN (${loop.condition.decompile(data)})")
    }

    override fun visitThrow(expression: IrThrow, data: String) {
        printer.println("$THROW_TOKEN ${expression.value.decompile(data)}")
    }


    override fun visitTry(aTry: IrTry, data: String) {
        if (data == RETURN_TOKEN) {
            printer.printWithNoIndent(TRY_TOKEN)
        } else {
            printer.print(TRY_TOKEN)
        }
        withBracesLn {
            aTry.tryResult.accept(this, data)
        }
        aTry.catches.decompileElements(data)
        if (aTry.finallyExpression != null) {
            printer.print("$FINALLY_TOKEN ")
            withBracesLn {
                aTry.finallyExpression!!.accept(this, data)
            }
        }
    }

    override fun visitCatch(aCatch: IrCatch, data: String) {
        printer.print("$CATCH_TOKEN (${aCatch.catchParameter.decompile(data)}) ")
        withBracesLn {
            aCatch.result.accept(this, data)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: String) {
        with(expression) {
            when (operator) {
                IMPLICIT_COERCION_TO_UNIT -> printer.println(argument.decompile(data))
                INSTANCEOF -> printer.print("(${argument.decompile(data)} is ${typeOperand.toKotlinType()})")
                NOT_INSTANCEOF -> printer.print("${argument.decompile(data)} !is ${typeOperand.toKotlinType()}")
                //TODO добавить операторы приведения CAST
                else -> TODO("Unexpected type operator $operator!")
            }

        }
    }

    override fun visitBreak(jump: IrBreak, data: String) {
        //TODO отрисовать label break@loop если label не пуст
        printer.println("break")
    }

    override fun visitContinue(jump: IrContinue, data: String) {
        //TODO отрисовать label continue@loop если label не пуст
        printer.println("continue")
    }

    override fun visitVararg(expression: IrVararg, data: String) {
        printer.print(expression.elements.joinToString(", ") { it.decompile(data) })
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
        if (declaration.fqName != FqName.ROOT) {
            printer.println("package ${declaration.fqName.asString()}\n")
        }
        declaration.acceptChildren(this, data)
    }

    override fun visitComposite(expression: IrComposite, data: String) {
        expression.acceptChildren(this, data)
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: String) {
        printer.print("*${spread.expression.decompile(data)}")
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: String) {
        printer.println(
            (0 until expression.valueArgumentsCount)
                .map { expression.getValueArgument(it)?.decompile(data) }
                .joinToString(",", "(", ")")
                .takeIf { expression.valueArgumentsCount > 0 } ?: EMPTY_TOKEN
        )
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: String) {
        printer.printWithNoIndent("${expression.type.obtainTypeDescription()}.${expression.symbol.owner.name()}")
    }

    override fun visitEnumEntry(declaration: IrEnumEntry, data: String) {
        printer.print("${declaration.name()}${declaration.initializerExpression?.decompile(data)}")
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: String) {
        //TODO умеем пока только сслыки на top-level функции из текущего скоупа
        printer.printWithNoIndent("::${expression.symbol.owner.name()}")
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: String) {
        if (expression.origin == LAMBDA) {
            indented {
                val body = expression.function.body
                if (body?.statements?.size == 1) {
                    val firstStatement = body?.statements?.get(0)
                    if (firstStatement is IrReturn) {
                        val returnStatementCall = (firstStatement as IrReturnImpl).value as IrCall
                        val leftFromArrowParams = (0 until returnStatementCall.valueArgumentsCount)
                            .map { returnStatementCall.getValueArgument(it)?.decompile("") }
                            .toHashSet()
                            .apply { add(returnStatementCall.dispatchReceiver?.decompile("")) }
                            .filterNotNull()
                        if (leftFromArrowParams.isNotEmpty()) {
                            printer.printWithNoIndent("{ ${leftFromArrowParams.joinToString(", ")}  -> ${firstStatement.decompile("lambda")} }")
                        } else {
                            printer.printlnWithNoIndent("{ ${firstStatement.decompile("lambda")} }")
                        }
//                        .joinToString(", ", postfix = " -> ") + firstStatement.decompile("lambda")
//                    printer.printWithNoIndent("{ $result }")
                    }
                } else {
                    body?.accept(this, data)
                }
            }
        } else {
            TODO("Visitor implemented only for lambdas!")
        }
//            ?.decompile()
//            ?.split("\n")
//            ?.forEach { indented { printer.println(it) } }

    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: String) = TODO()

    override fun visitErrorCallExpression(expression: IrErrorCallExpression, data: String) = TODO()

    override fun visitTypeParameter(declaration: IrTypeParameter, data: String) = TODO()

    //TODO если его заменяют obtainPrimaryCtor и obtainSecondaryCtor - выпилить
    override fun visitConstructor(declaration: IrConstructor, data: String) = TODO()

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression, data: String) = TODO()

    override fun visitElement(element: IrElement, data: String) = TODO()

}

