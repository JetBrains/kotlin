/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.evaluate

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import kotlin.reflect.full.memberFunctions

val DEST_FILE: File = File("compiler/frontend.common/src/org/jetbrains/kotlin/resolve/constants/evaluate/OperationsMapGenerated.kt")
private val EXCLUDED_FUNCTIONS: List<String> = listOf("rangeTo", "rangeUntil", "hashCode", "inc", "dec", "subSequence")

fun main() {
    GeneratorsFileUtil.writeFileIfContentChanged(DEST_FILE, generate())
}

fun generate(): String {
    val sb = StringBuilder()
    val p = Printer(sb)

    generateHeader(p)

    val (unaryOperationsMap, binaryOperationsMap) = getOperationMaps()

    generateUnaryOp(p, unaryOperationsMap)
    generateBinaryOp(p, binaryOperationsMap)
    generateBinaryOpCheck(p, binaryOperationsMap)

    return sb.toString()
}

private fun getOperationMaps(): Pair<ArrayList<Triple<String, List<String>, Boolean>>, ArrayList<Pair<String, List<String>>>> {
    val unaryOperationsMap = arrayListOf<Triple<String, List<String>, Boolean>>()
    val binaryOperationsMap = arrayListOf<Pair<String, List<String>>>()

    val builtIns = DefaultBuiltIns.Instance

    @Suppress("UNCHECKED_CAST")
    val allPrimitiveTypes = builtIns.builtInsPackageScope.getContributedDescriptors()
        .filter { it is ClassDescriptor && KotlinBuiltIns.isPrimitiveType(it.defaultType) } as List<ClassDescriptor>

    val integerTypes = allPrimitiveTypes.map { it.defaultType }.filter { it.isIntegerType() }
    val fpTypes = allPrimitiveTypes.map { it.defaultType }.filter { it.isFpType() }

    for (descriptor in allPrimitiveTypes + builtIns.string) {
        @Suppress("UNCHECKED_CAST")
        val functions = descriptor.getMemberScope(listOf()).getContributedDescriptors()
            .filter { it is CallableDescriptor && !EXCLUDED_FUNCTIONS.contains(it.getName().asString()) } as List<CallableDescriptor>

        for (function in functions) {
            val parametersTypes = function.getParametersTypes()

            when (parametersTypes.size) {
                1 -> unaryOperationsMap.add(Triple(function.name.asString(), parametersTypes, function is FunctionDescriptor))
                2 -> binaryOperationsMap.add(function.name.asString() to parametersTypes)
                else -> throw IllegalStateException(
                    "Couldn't add following method from builtins to operations map: ${function.name} in class ${descriptor.name}"
                )
            }
        }
    }

    unaryOperationsMap.add(Triple("code", listOf(builtIns.charType.toString()), false))

    for (type in integerTypes) {
        for (otherType in integerTypes) {
            val parameters = listOf(type, otherType).map {it.toString()}
            binaryOperationsMap.add("mod" to parameters)
            binaryOperationsMap.add("floorDiv" to parameters)
        }
    }

    for (type in fpTypes) {
        for (otherType in fpTypes) {
            val parameters = listOf(type, otherType).map {it.toString()}
            binaryOperationsMap.add("mod" to parameters)
        }
    }

    val unsignedClasses = listOf(UInt::class, ULong::class, UByte::class, UShort::class)
    for (unsignedClass in unsignedClasses) {
        unsignedClass.memberFunctions
            .filter { !EXCLUDED_FUNCTIONS.contains(it.name) }
            .forEach { function ->
                val args = function.parameters.map { it.type.toString().removePrefix("kotlin.") }
                when (function.parameters.size) {
                    1 -> unaryOperationsMap.add(Triple(function.name, args, true))
                    2 -> binaryOperationsMap.add(function.name to args)
                }
            }
    }

    val uintConversionExtensions = mapOf(
        "Long" to listOf("toULong", "toUInt", "toUShort", "toUByte"),
        "Int" to listOf("toULong", "toUInt", "toUShort", "toUByte"),
        "Short" to listOf("toULong", "toUInt", "toUShort", "toUByte"),
        "Byte" to listOf("toULong", "toUInt", "toUShort", "toUByte"),
        "Double" to listOf("toULong", "toUInt"),
        "Float" to listOf("toULong", "toUInt"),
    )

    for ((type, extensions) in uintConversionExtensions) {
        for (extension in extensions) {
            unaryOperationsMap.add(Triple(extension, listOf(type), true))
        }
    }

    return Pair(unaryOperationsMap, binaryOperationsMap)
}

private fun generateHeader(p: Printer) {
    p.println(File("license/COPYRIGHT_HEADER.txt").readText())
    p.println("@file:Suppress(\"DEPRECATION\", \"DEPRECATION_ERROR\")")

    p.println()
    p.println("package org.jetbrains.kotlin.resolve.constants.evaluate")
    p.println()
    p.println("import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType.*")
    p.println("import java.math.BigInteger")
    p.println()
    p.println("/** This file is generated by `./gradlew generateOperationsMap`. DO NOT MODIFY MANUALLY */")
    p.println()
}

private fun generateUnaryOp(
    p: Printer,
    unaryOperationsMap: ArrayList<Triple<String, List<String>, Boolean>>,
) {
    p.println("fun evalUnaryOp(name: String, type: CompileTimeType, value: Any): Any? {")
    p.pushIndent()
    p.println("when (type) {")
    p.pushIndent()
    for ((type, operations) in unaryOperationsMap.groupBy { (_, parameters, _) -> parameters.single() }) {
        p.println("${type.formatCompileTypeEnum()} -> when (name) {")
        p.pushIndent()
        for ((name, _, isFunction) in operations) {
            val parenthesesOrBlank = if (isFunction) "()" else ""
            p.println("\"$name\" -> return (value as ${type}).$name$parenthesesOrBlank")
        }
        p.popIndent()
        p.println("}")
    }
    p.println("else -> {}")
    p.popIndent()
    p.println("}")
    p.println("return null")
    p.popIndent()
    p.println("}")
    p.println()
}

private fun generateBinaryOp(
    p: Printer,
    binaryOperationsMap: ArrayList<Pair<String, List<String>>>,
) {
    p.println("fun evalBinaryOp(name: String, leftType: CompileTimeType, left: Any, rightType: CompileTimeType, right: Any): Any? {")
    p.pushIndent()
    p.println("when (leftType) {")
    p.pushIndent()
    for ((leftType, operationsOnThisLeftType) in binaryOperationsMap.groupBy { (_, parameters) -> parameters.first() }) {
        p.println("${leftType.formatCompileTypeEnum()} -> when (rightType) {")
        p.pushIndent()
        for ((rightType, operations) in operationsOnThisLeftType.groupBy { (_, parameters) -> parameters[1] }) {
            p.println("${rightType.formatCompileTypeEnum()} -> when (name) {")
            p.pushIndent()
            for ((name, _) in operations) {
                val castToRightType = if (rightType.removeSuffix("?") == "Any") "" else " as ${rightType}"
                p.println("\"$name\" -> return (left as ${leftType}).$name(right$castToRightType)")
            }
            p.popIndent()
            p.println("}")
        }
        p.println("else -> {}")
        p.popIndent()
        p.println("}")
    }
    p.println("else -> {}")
    p.popIndent()
    p.println("}")
    p.println("return null")
    p.popIndent()
    p.println("}")
    p.println()
}

// TODO: Can be dropped with K1
private fun generateBinaryOpCheck(
    p: Printer,
    binaryOperationsMap: ArrayList<Pair<String, List<String>>>,
) {
    p.println("fun checkBinaryOp(")
    p.println("    name: String, leftType: CompileTimeType, left: BigInteger, rightType: CompileTimeType, right: BigInteger")
    p.println("): BigInteger? {")
    p.pushIndent()
    p.println("when (leftType) {")
    p.pushIndent()
    val checkedBinaryOperations =
        binaryOperationsMap.filter { (name, parameters) -> getBinaryCheckerName(name, parameters[0], parameters[1]) != null }
    for ((leftType, operationsOnThisLeftType) in checkedBinaryOperations.groupBy { (_, parameters) -> parameters.first() }) {
        p.println("${leftType.formatCompileTypeEnum()} -> when (rightType) {")
        p.pushIndent()
        for ((rightType, operations) in operationsOnThisLeftType.groupBy { (_, parameters) -> parameters[1] }) {
            p.println("${rightType.formatCompileTypeEnum()} -> when (name) {")
            p.pushIndent()
            for ((name, _) in operations) {
                val checkerName = getBinaryCheckerName(name, leftType, rightType)!!
                p.println("\"$name\" -> return left.$checkerName(right)")
            }
            p.popIndent()
            p.println("}")
        }
        p.println("else -> {}")
        p.popIndent()
        p.println("}")
    }
    p.println("else -> {}")
    p.popIndent()
    p.println("}")
    p.println("return null")
    p.popIndent()
    p.println("}")
}

private fun getBinaryCheckerName(name: String, leftType: String, rightType: String): String? {
    val integerTypes = listOf("Int", "Short", "Byte", "Long")
    if (!integerTypes.contains(leftType) || !integerTypes.contains(rightType)) return null

    return when (name) {
        "plus" -> "add"
        "minus" -> "subtract"
        "div" -> "divide"
        "times" -> "multiply"
        "rem", "xor", "or", "and" -> name
        else -> null
    }
}

private fun KotlinType.isIntegerType(): Boolean =
    KotlinBuiltIns.isInt(this) || KotlinBuiltIns.isShort(this) || KotlinBuiltIns.isByte(this) || KotlinBuiltIns.isLong(this)

private fun KotlinType.isFpType(): Boolean =
    KotlinBuiltIns.isDouble(this) || KotlinBuiltIns.isFloat(this)

private fun CallableDescriptor.getParametersTypes(): List<String> =
    (listOf((containingDeclaration as ClassDescriptor).defaultType.toString()) +
            valueParameters.map { it.type.makeNotNullable().toString() })

private fun String.formatCompileTypeEnum(): String = this.uppercase().removeSuffix("?")

private val KotlinType.typeName: String
    get(): String = constructor.declarationDescriptor!!.name.asString()
