/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler.util

import org.jetbrains.kotlin.decompiler.DecompileIrTreeVisitor
import org.jetbrains.kotlin.decompiler.decompile
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.typeUtil.isInterface

const val EMPTY_TOKEN = ""
const val OROR_TOKEN = "||"
const val ANDAND_TOKEN = "&&"
const val EQ_TOKEN = "="
const val PLUS_TOKEN = "+"
const val MINUS_TOKEN = "-"
const val MUL_TOKEN = "*"
const val DIV_TOKEN = "/"
const val PERC_TOKEN = "%"
const val EQEQ_TOKEN = "=="
const val EQEQEQ_TOKEN = "==="
const val GT_TOKEN = ">"
const val LT_TOKEN = "<"
const val GTEQ_TOKEN = ">="
const val LTEQ_TOKEN = "<="
const val THIS_TOKEN = "this"
const val TRY_TOKEN = "try"
const val CATCH_TOKEN = "catch"
const val CTOR_TOKEN = "constructor"
const val INIT_TOKEN = "init"
const val FINALLY_TOKEN = "finally"
const val TYPEALIAS_TOKEN = "typealias"
const val SUPER_TOKEN = "super"
const val THROW_TOKEN = "throw"
const val CLASS_TOKEN = "class"
const val INTERFACE_TOKEN = "interface"
const val COMPANION_TOKEN = "companion object"
const val OBJECT_TOKEN = "object"
const val INNER_TOKEN = "inner"
const val INLINE_TOKEN = "inline"
const val DATA_TOKEN = "data"
const val EXTERNAL_TOKEN = "external"
const val ANNOTATION_TOKEN = "annotation class"
const val ENUM_TOKEN = "enum"
const val IF_TOKEN = "if"
const val WHEN_TOKEN = "when"
const val ELSE_TOKEN = "else"
const val DO_TOKEN = "do"
const val WHILE_TOKEN = "while"
const val RETURN_TOKEN = "return"
const val OVERRIDE_TOKEN = "override"
const val FUN_TOKEN = "fun"

internal inline fun DecompileIrTreeVisitor.withBracesLn(body: () -> Unit) {
    printer.printlnWithNoIndent(" {")
    indented(body)
    printer.println("} ")
}

internal inline fun DecompileIrTreeVisitor.withBraces(body: () -> Unit) {
    printer.printlnWithNoIndent(" {")
    indented(body)
    printer.print("} ")
}


internal inline fun DecompileIrTreeVisitor.indented(body: () -> Unit) {
    printer.pushIndent()
    body()
    printer.popIndent()
}

internal fun obtainFlagsList(vararg flags: String?) =
    flags.filterNotNull().run {
        if (isNotEmpty())
            joinToString(separator = " ")
        else
            ""
    }

internal inline fun IrDeclaration.name(): String = descriptor.name.asString()

internal fun IrTypeAlias.obtainTypeAliasFlags(): String =
    obtainFlagsList(
        "actual".takeIf { isActual }
    )

internal fun IrClass.obtainModality(): String? =
    when {
        modality != Modality.FINAL && !isInterface -> "${modality.name.toLowerCase()} "
        else -> null
    }

internal fun IrProperty.obtainModality(): String? =
    when {
        modality != Modality.FINAL -> "${modality.name.toLowerCase()} "
        else -> null
    }

internal fun IrSimpleFunction.obtainModality(): String? =
    when {
        modality != Modality.FINAL -> "${modality.name.toLowerCase()} "
        else -> null
    }


internal fun IrCall.obtainUnaryOperatorCall(): String {
    var result = when (origin) {
        UPLUS -> PLUS_TOKEN
        UMINUS -> MINUS_TOKEN
        else -> TODO("Not implemented for ${origin.toString()} unary operator")
    }
    return "$result${dispatchReceiver?.decompile() ?: ""}"
}

internal fun IrCall.obtainBinaryOperatorCall(): String {
    var result = dispatchReceiver?.decompile() ?: ""
    result += " " + when (origin) {
        PLUS -> PLUS_TOKEN
        MINUS -> MINUS_TOKEN
        MUL -> MUL_TOKEN
        DIV -> DIV_TOKEN
        PERC -> PERC_TOKEN
        ANDAND -> ANDAND_TOKEN
        OROR -> OROR_TOKEN
        else -> TODO("Not implemented for ${origin.toString()} binary operator")
    }
    return "$result ${getValueArgument(0)?.decompile()}"
}

//TODO разобраться когда тут вызов compareTo, а когда реальное сравнение
internal fun IrCall.obtainComparisonOperatorCall(): String {
    val leftOperand = if (dispatchReceiver == null) dispatchReceiver else getValueArgument(0)
    val rightOperand = if (dispatchReceiver != null) getValueArgument(0) else getValueArgument(1)
    val sign = " " + when (origin) {
        EQEQ -> EQEQ_TOKEN
        GT -> GT_TOKEN
        LT -> LT_TOKEN
        GTEQ -> GTEQ_TOKEN
        LTEQ -> LTEQ_TOKEN
        else -> TODO("Not implemented for ${origin.toString()} comparison operator")
    }
    return "${leftOperand?.decompile()} $sign ${rightOperand?.decompile()}"
}

internal fun IrCall.obtainNotEqCall(): String =
    if (symbol.owner.name().toLowerCase() != "not") {
        "${getValueArgument(0)?.decompile()} $EQEQ_TOKEN ${getValueArgument(1)?.decompile()}"
    } else {
        "(${dispatchReceiver?.decompile()}).${obtainNameWithArgs()}"
    }

internal fun IrCall.obtainNameWithArgs(): String {
    var result = symbol.owner.name()
    result += (0 until valueArgumentsCount).mapNotNull {
        getValueArgument(it)?.decompile()
    }.joinToString(separator = ", ", prefix = "(", postfix = ")")
    return result
}


internal fun IrCall.obtainCall(): String {
    return when (origin) {
        UPLUS, UMINUS -> obtainUnaryOperatorCall()
        PLUS, MINUS, MUL, DIV, PERC, ANDAND, OROR -> obtainBinaryOperatorCall()
        // === и !==
        EQEQ, GT, LT, GTEQ, LTEQ -> obtainComparisonOperatorCall()
        EXCLEQ -> obtainNotEqCall()
        GET_PROPERTY -> obtainGetPropertyCall()
        // сюда прилетает только правая часть, левая разбирается в visitSetVariable
        PLUSEQ, MINUSEQ, MULTEQ, DIVEQ -> getValueArgument(0)?.decompile() ?: ""
        // Для присваивания свойстам в конструкторах
        EQ -> obtainEqCall()
        else -> {
            var result = ""
            if (dispatchReceiver != null) {
                result += if (superQualifierSymbol != null) {
                    "$SUPER_TOKEN."
                } else {
                    "${dispatchReceiver!!.decompile()}."
                }
            } else if (extensionReceiver != null) {
                result += "${extensionReceiver!!.decompile()}."
            }

            result + obtainNameWithArgs()
        }
    }
}


internal fun IrCall.obtainEqCall(): String =
    "${dispatchReceiver?.decompile()}.${symbol.owner.decompile()} = ${getValueArgument(0)?.decompile()}"

internal fun IrCall.obtainGetPropertyCall(): String {
    val fullName = symbol.owner.name()
    val regex = """<get-(.+)>""".toRegex()
    val matchResult = regex.find(fullName)
    val propName = matchResult?.groups?.get(1)?.value

    return "${dispatchReceiver?.decompile()}.$propName" //if (!propName.isNullOrEmpty()) { && primaryCtor.isPrimaryCtorArg(propName)) {
//        propName
//    } else {
//        "${dispatchReceiver?.decompile()}.$propName"
//    }
}

internal fun IrConstructor.obtainValueParameterTypes(): String =
    ArrayList<String>().apply {
        valueParameters.mapTo(this) {
            var argDefinition = "${"val ".takeIf { isPrimary }.orEmpty()}${it.name}: ${it.type.toKotlinType()}"
            if (it.hasDefaultValue()) {
                argDefinition += " = ${it.defaultValue!!.decompile()}"
            }
            argDefinition
        }
    }.joinToString(separator = ", ", prefix = "(", postfix = ")")


internal fun IrFunction.obtainValueParameterTypes(): String =
    ArrayList<String>().apply {
        valueParameters.mapTo(this) {
            var argDefinition = "${it.name}: ${it.type.toKotlinType()}"
            if (it.defaultValue != null) {
                argDefinition += " = ${it.defaultValue!!.decompile()}"
            }
            argDefinition
        }
    }.joinToString(separator = ", ", prefix = "(", postfix = ")")

internal inline fun IrDeclarationWithVisibility.obtainVisibility(): String =
    when (visibility) {
        Visibilities.PUBLIC -> EMPTY_TOKEN
        else -> "${visibility.name.toLowerCase()} "
    }


internal fun IrClass.obtainInheritance(): String {
    val implementedInterfaces = superTypes
        .filter { !it.isAny() && it.toKotlinType().isInterface() }
        .map {
            it.toKotlinType().toString()
        }
    return implementedInterfaces.joinToString(", ")
}

internal fun concatenateConditions(condition: IrExpression): String {
    var result = ""
    when (condition) {
        is IrIfThenElseImpl -> {
            val firstBranch = condition.branches[0]
            result += "(${concatenateConditions(firstBranch.condition)})"
            if (firstBranch.result !is IrConst<*>) {
                when (condition.origin) {
                    ANDAND -> {
                        result += " $ANDAND_TOKEN "
                        result += "(${concatenateConditions(firstBranch.result)})"
                    }
                    OROR -> {
                        result += " $OROR_TOKEN "
                        result += "(${concatenateConditions(firstBranch.result)})"
                    }
                    else -> {
                        TODO()
                    }
                }
            }
        }
        is IrCallImpl -> {
            return condition.decompile()
        }
    }
    return result
}

internal fun IrClass.obtainDeclarationStr(): String =
    ArrayList<String?>().apply {
        add(obtainVisibility())
        add(obtainModality())
        add(obtainClassFlags())
        add("${name()}${obtainTypeParameters()}")
    }.filterNot { it.isNullOrEmpty() }.joinToString(" ")

internal fun IrClass.obtainClassFlags() =
    obtainFlagsList(
        CLASS_TOKEN.takeIf { isClass },
        INTERFACE_TOKEN.takeIf { isInterface },
        COMPANION_TOKEN.takeIf { isCompanion },
        OBJECT_TOKEN.takeIf { isObject },
        INNER_TOKEN.takeIf { isInner },
        INLINE_TOKEN.takeIf { isInline },
        DATA_TOKEN.takeIf { isData },
        EXTERNAL_TOKEN.takeIf { isExternal },
        ANNOTATION_TOKEN.takeIf { isAnnotationClass },
        ENUM_TOKEN.takeIf { isEnumClass }
    )

internal fun IrClass.isPrimaryCtorArg(argName: String): Boolean {
    val primaryCtor = primaryConstructor
    val primaryCtorArgNames = primaryCtor?.valueParameters?.map { it.name() }
    return !primaryCtorArgNames.isNullOrEmpty() && primaryCtorArgNames.contains(argName)
}

internal fun IrVariable.obtainVariableFlags(): String =
    obtainFlagsList(
        "const".takeIf { isConst },
        "lateinit".takeIf { isLateinit },
        if (isVar) "var" else "val"
    )

internal fun IrProperty.obtainPropertyFlags() =
    obtainFlagsList(
        "external".takeIf { isExternal },
        "const".takeIf { isConst },
        "lateinit".takeIf { isLateinit },
        "delegated".takeIf { isDelegated },
        if (isVar) "var" else "val"
    )

internal fun IrValueParameter.obtainValueParameterFlags(): String =
    obtainFlagsList(
        "vararg".takeIf { varargElementType != null },
        "crossinline".takeIf { isCrossinline },
        "noinline".takeIf { isNoinline }
    )

internal fun IrTypeParametersContainer.obtainTypeParameters(): String =
    if (typeParameters.isEmpty())
        ""
    else
        typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") {
            it.obtain()
        }.trim()

internal fun IrTypeParameter.obtain() = "${if (variance.label.isNotEmpty()) variance.label + " " else ""}${name()}"

//TODO посмотреть где это используется (особенно с IrTypeAbbreviation)
internal fun IrTypeArgument.obtain() =
    when (this) {
        is IrStarProjection -> "*"
        is IrTypeProjection -> "${if (variance.label.isNotEmpty()) variance.label + " " else ""}${type.toKotlinType()}"
        else -> throw AssertionError("Unexpected IrTypeArgument: $this")
    }

internal fun IrSimpleFunction.isOverriden() =
    overriddenSymbols.isNotEmpty() && overriddenSymbols.map { it.owner.name() }.contains(name())

internal fun IrSimpleFunction.obtainSimpleFunctionFlags(): String =
    obtainFlagsList(
        "tailrec".takeIf { isTailrec },
        "inline".takeIf { isInline },
        "external".takeIf { isExternal },
        "suspend".takeIf { isSuspend }
    )

internal fun IrSimpleFunction.obtainCustomGetter() {
    when (origin) {
        IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR -> {
        }
        else -> TODO("Not yet implemented for custom getter!")
    }
}

internal fun IrSimpleFunction.obtainCustomSetter() {
    when (origin) {
        IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR -> {
        }
        else -> TODO("Not yet implemented for custom setter!")
    }
}

internal fun decompileAnnotations(element: IrAnnotationContainer) {
    //TODO правильно рендерить аннотации
}


internal fun IrFunction.obtainFunctionName(): String {
    var result = ""
    if (extensionReceiverParameter != null) {
        result += extensionReceiverParameter?.type?.toKotlinType().toString()
        result += "."
    }
    return result + name()
}