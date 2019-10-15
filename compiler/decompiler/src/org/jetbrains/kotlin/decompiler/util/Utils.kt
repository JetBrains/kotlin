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
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.expressions.impl.IrIfThenElseImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

val OPERATOR_TOKENS = mutableMapOf<IrStatementOrigin, String>().apply {
    set(OROR, "||")
    set(ANDAND, "&&")
    set(PLUS, "+")
    set(MINUS, "-")
    set(UPLUS, "+")
    set(UMINUS, "-")
    set(MUL, "*")
    set(DIV, "/")
    set(PERC, "%")
    set(EQ, "=")
    set(PLUSEQ, "+=")
    set(MINUSEQ, "-=")
    set(MULTEQ, "*=")
    set(DIVEQ, "/=")
    set(PERCEQ, "%=")
    set(EQEQ, "==")
    set(EQEQEQ, "===")
    set(EXCLEQ, "!=")
    set(EXCLEQEQ, "!==")
    set(GT, ">")
    set(LT, "<")
    set(GTEQ, ">=")
    set(LTEQ, "<=")
    set(ELVIS, "?:")
    set(RANGE, "..")
    set(PREFIX_INCR, "++")
    set(PREFIX_DECR, "--")
    set(POSTFIX_INCR, "++")
    set(POSTFIX_DECR, "--")
}

val OPERATOR_NAMES = setOf("less", "lessOrEqual", "greater", "greaterOrEqual", "EQEQ", "EQEQEQ")

const val EMPTY_TOKEN = ""
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
            EMPTY_TOKEN
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


internal fun IrCall.obtainUnaryOperatorCall(): String = "${OPERATOR_TOKENS[origin]}${dispatchReceiver?.decompile() ?: EMPTY_TOKEN}"


internal fun IrCall.obtainBinaryOperatorCall(): String =
    "${dispatchReceiver?.decompile() ?: EMPTY_TOKEN} ${OPERATOR_TOKENS[origin]} ${getValueArgument(0)?.decompile()}"


//TODO разобраться когда тут вызов compareTo, а когда реальное сравнение
internal fun IrCall.obtainComparisonOperatorCall(): String {
    val leftOperand = if (dispatchReceiver != null) dispatchReceiver else getValueArgument(0)
    val rightOperand = if (dispatchReceiver != null) getValueArgument(0) else getValueArgument(1)
    if (symbol.owner.name() in OPERATOR_NAMES) {
        val sign = OPERATOR_TOKENS[origin]
        return "${leftOperand?.decompile()} $sign ${rightOperand?.decompile()}"
    } else {
        return "${leftOperand?.decompile()}.${symbol.owner.name()}(${rightOperand?.decompile()})"
    }
}

internal fun IrCall.obtainNotEqCall(): String =
    if (symbol.owner.name().toLowerCase() != "not") {
        "${getValueArgument(0)?.decompile()} ${OPERATOR_TOKENS[origin]} ${getValueArgument(1)?.decompile()}"
    } else {
        dispatchReceiver?.decompile() ?: EMPTY_TOKEN
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
        PLUSEQ, MINUSEQ, MULTEQ, DIVEQ -> getValueArgument(0)?.decompile() ?: EMPTY_TOKEN
        // Для присваивания свойстам в конструкторах
        EQ -> obtainEqCall()
        else -> {
            var result = EMPTY_TOKEN
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
            var argDefinition =
                listOf(
                    it.obtainValueParameterFlags(),
                    "val".takeIf { isPrimary }.orEmpty(),
                    it.name(),
                    ": ",
                    (it.varargElementType?.toKotlinType() ?: it.type.toKotlinType()).toString()
                )
                    .filterNot { it.isEmpty() }.joinToString(" ")
            if (it.hasDefaultValue()) {
                argDefinition += " = ${it.defaultValue!!.decompile()}"
            }
            argDefinition
        }
    }.joinToString(separator = ", ", prefix = "(", postfix = ")")


internal fun IrFunction.obtainValueParameterTypes(): String =
    ArrayList<String>().apply {
        valueParameters.mapTo(this) {
            var argDefinition = listOf(
                it.obtainValueParameterFlags(),
                "${it.name()}:",
                (it.varargElementType?.toKotlinType() ?: it.type.toKotlinType()).toString()
            )
                .filter { it.isNotEmpty() }.joinToString(" ")
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

internal fun concatenateConditions(branch: IrBranch): String {
    when (branch.condition) {
        is IrIfThenElseImpl -> {
            val irIfThenElseImplBranch = branch.condition as IrIfThenElseImpl
            val firstBranch = irIfThenElseImplBranch.branches[0]
            val secondBranch = irIfThenElseImplBranch.branches[1]
            when (irIfThenElseImplBranch.origin) {
                OROR -> return listOf(
                    "(${secondBranch.result.decompile()})",
                    OPERATOR_TOKENS[irIfThenElseImplBranch.origin],
                    "(${concatenateConditions(firstBranch)})"
                ).joinToString(" ")
                ANDAND -> {
                    var result = "(${concatenateConditions(firstBranch)})"
                    if (firstBranch.result !is IrConst<*>) {
                        result += " ${OPERATOR_TOKENS[irIfThenElseImplBranch.origin]} "
                        result += "(${firstBranch.result.decompile()})"
                    }
                    return result
                }
                else -> TODO()
            }
        }
        else -> {
            return branch.condition.decompile()
        }
    }
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
        EMPTY_TOKEN
    else
        typeParameters.joinToString(separator = ", ", prefix = "<", postfix = ">") {
            it.obtain()
        }.trim()

private fun IrTypeParameter.variance() = variance.label
private fun IrTypeParameter.bound() =
    superTypes
        .filterNot { it.isNullableAny() }
        .map { it.toKotlinType().toString() }
        .ifNotEmpty { joinToString(", ", prefix = " : ") } ?: EMPTY_TOKEN

internal fun IrTypeParameter.obtain() =
    listOf(variance(), name(), bound())
        .filter { it.isNotEmpty() }
        .joinToString(" ")

//TODO посмотреть где это используется (особенно с IrTypeAbbreviation)
internal fun IrTypeArgument.obtain() =
    when (this) {
        is IrStarProjection -> "*"
        is IrTypeProjection -> "${if (variance.label.isNotEmpty()) variance.label + " " else EMPTY_TOKEN}${type.toKotlinType()}"
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
    var result = EMPTY_TOKEN
    if (extensionReceiverParameter != null) {
        result += extensionReceiverParameter?.type?.toKotlinType().toString()
        result += "."
    }
    return result + name()
}