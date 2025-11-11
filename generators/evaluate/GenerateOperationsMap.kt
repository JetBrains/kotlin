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
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.FqName
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

    generateCanEvalOpFunction(p, unaryOperationsMap + binaryOperationsMap)

    return sb.toString()
}

private fun getOperationMaps(): Pair<ArrayList<Operation>, ArrayList<Operation>> {
    val unaryOperationsMap = arrayListOf<Operation>()
    val binaryOperationsMap = arrayListOf<Operation>()

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
            val parametersTypes = function.getParametersTypes().map { it.typeName }
            val classDescriptor = function.containingDeclaration as ClassDescriptor

            when (parametersTypes.size) {
                1 -> unaryOperationsMap.add(
                    Operation(
                        className = classDescriptor.name.asString(),
                        name = function.name.asString(),
                        parameterTypes = parametersTypes,
                        isFunction = function is FunctionDescriptor
                    )
                )
                2 -> binaryOperationsMap.add(
                    Operation(
                        className = classDescriptor.name.asString(),
                        name = function.name.asString(),
                        parameterTypes = parametersTypes
                    )
                )
                else -> throw IllegalStateException(
                    "Couldn't add following method from builtins to operations map: ${function.name} in class ${descriptor.name}"
                )
            }
        }
    }

    unaryOperationsMap.add(
        Operation(
            className = "Char",
            name = "code",
            parameterTypes = listOf("Char"),
            isFunction = false
        )
    )

    for (type in integerTypes) {
        for (otherType in integerTypes) {
            val parameters = listOf(type, otherType).map { it.typeName }
            binaryOperationsMap.add(Operation(className = null, name = "mod", parameterTypes = parameters))
            binaryOperationsMap.add(Operation(className = null, name = "floorDiv", parameterTypes = parameters))
        }
    }

    for (type in fpTypes) {
        for (otherType in fpTypes) {
            val parameters = listOf(type, otherType).map { it.typeName }
            binaryOperationsMap.add(Operation(className = null, name = "mod", parameterTypes = parameters))
        }
    }

    val unsignedClasses = listOf(UInt::class, ULong::class, UByte::class, UShort::class)
    for (unsignedClass in unsignedClasses) {
        unsignedClass.memberFunctions
            .filter { !EXCLUDED_FUNCTIONS.contains(it.name) }
            .forEach { function ->
                val args = function.parameters.map { it.type.toString().removePrefix("kotlin.") }
                when (function.parameters.size) {
                    1 -> unaryOperationsMap.add(Operation(className = unsignedClass.simpleName, name = function.name, parameterTypes = args))
                    2 -> binaryOperationsMap.add(Operation(className = unsignedClass.simpleName, name = function.name, parameterTypes = args))
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
        for ((extension, declaredIn) in extensions.zip(listOf("ULong", "UInt", "UShort", "UByte"))) {
            unaryOperationsMap.add(Operation(className = declaredIn, name = extension, parameterTypes = listOf(type)))
        }
    }

    return Pair(unaryOperationsMap, binaryOperationsMap)
}

private fun generateHeader(p: Printer) {
    p.println(File("license/COPYRIGHT_HEADER.txt").readText())
    p.println("@file:Suppress(\"DEPRECATION\", \"DEPRECATION_ERROR\", \"REDUNDANT_CALL_OF_CONVERSION_METHOD\")")

    p.println()
    p.println("package org.jetbrains.kotlin.resolve.constants.evaluate")
    p.println()
    p.println("import org.jetbrains.kotlin.name.CallableId")
    p.println("import org.jetbrains.kotlin.resolve.constants.evaluate.CompileTimeType.*")
    p.println("import java.math.BigInteger")
    p.println()
    p.println("/** This file is generated by `./gradlew generateOperationsMap`. DO NOT MODIFY MANUALLY */")
    p.println()
}

private fun generateUnaryOp(
    p: Printer,
    unaryOperationsMap: ArrayList<Operation>,
) {
    p.println("fun evalUnaryOp(name: String, type: CompileTimeType, value: Any): Any? {")
    p.pushIndent()
    p.println("when (type) {")
    p.pushIndent()
    for ((type, operations) in unaryOperationsMap.groupBy { it.parameterTypes.single() }) {
        p.println("${type.toCompilTimeTypeFormat()} -> when (name) {")
        p.pushIndent()
        for (operation in operations) {
            if (operation.customExpression != null) {
                p.println("\"${operation.name}\" -> return ${operation.customExpression}")
                continue
            }
            val parenthesesOrBlank = if (operation.isFunction) "()" else ""
            p.println("\"${operation.name}\" -> return (value as ${type}).${operation.name}$parenthesesOrBlank")
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
    binaryOperationsMap: ArrayList<Operation>,
) {
    p.println("fun evalBinaryOp(name: String, leftType: CompileTimeType, left: Any, rightType: CompileTimeType, right: Any): Any? {")
    p.pushIndent()
    p.println("when (leftType) {")
    p.pushIndent()
    for ((leftType, operationsOnThisLeftType) in binaryOperationsMap.groupBy { it.parameterTypes.first() }) {
        p.println("${leftType.toCompilTimeTypeFormat()} -> when (rightType) {")
        p.pushIndent()
        for ((rightType, operations) in operationsOnThisLeftType.groupBy { it.parameterTypes[1] }) {
            p.println("${rightType.toCompilTimeTypeFormat()} -> when (name) {")
            p.pushIndent()
            for (operation in operations) {
                val castToRightType = if (rightType == "Any" || rightType == "Any?") "" else " as ${rightType}"
                p.println("\"${operation.name}\" -> return (left as ${leftType}).${operation.name}(right$castToRightType)")
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

private fun generateCanEvalOpFunction(p: Printer, operations: List<Operation>) {
    p.println("private val knownOps = setOf(")
    p.pushIndent()
    for (operation in operations) {
        p.println("\"${operation.callableId}(${operation.parameterTypes.joinToString(", ") { it.toCompilTimeTypeFormat() }})\",")
    }
    p.popIndent()
    p.println(")")

    p.println("fun canEvalOp(callableId: CallableId, typeA: CompileTimeType, typeB: CompileTimeType?): Boolean {")
    p.pushIndent()
    p.println("val types = if (typeB != null) \"\$typeA, \$typeB\" else typeA.toString()")
    p.println("return knownOps.contains(\"\${callableId}(\$types)\")")
    p.popIndent()
    p.println("}")
}

// TODO, KT-75372: Can be dropped with K1
private fun generateBinaryOpCheck(
    p: Printer,
    binaryOperationsMap: ArrayList<Operation>,
) {
    p.println("fun checkBinaryOp(")
    p.println("    name: String, leftType: CompileTimeType, left: BigInteger, rightType: CompileTimeType, right: BigInteger")
    p.println("): BigInteger? {")
    p.pushIndent()
    p.println("when (leftType) {")
    p.pushIndent()
    val checkedBinaryOperations =
        binaryOperationsMap.filter { op -> getBinaryCheckerName(op.name, op.parameterTypes[0], op.parameterTypes[1]) != null }
    for ((leftType, operationsOnThisLeftType) in checkedBinaryOperations.groupBy { it.parameterTypes.first() }) {
        p.println("${leftType.toCompilTimeTypeFormat()} -> when (rightType) {")
        p.pushIndent()
        for ((rightType, operations) in operationsOnThisLeftType.groupBy { it.parameterTypes[1] }) {
            p.println("${rightType.toCompilTimeTypeFormat()} -> when (name) {")
            p.pushIndent()
            for (operation in operations) {
                val name = operation.name
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

private data class Operation(
    val packageName: String = "kotlin",
    val className: String?,
    val name: String,
    val parameterTypes: List<String>,
    val isFunction: Boolean = true,
    val customExpression: String? = null,
) {
    val callableId: CallableId
        get() = CallableId(FqName(packageName), className?.let{FqName(it)}, Name.identifier(name))
}

private fun KotlinType.isIntegerType(): Boolean =
    KotlinBuiltIns.isInt(this) || KotlinBuiltIns.isShort(this) || KotlinBuiltIns.isByte(this) || KotlinBuiltIns.isLong(this)

private fun KotlinType.isFpType(): Boolean =
    KotlinBuiltIns.isDouble(this) || KotlinBuiltIns.isFloat(this)

private fun CallableDescriptor.getParametersTypes(): List<KotlinType> =
    listOf((containingDeclaration as ClassDescriptor).defaultType) +
            valueParameters.map { it.type.makeNotNullable() }

// Formats the type to fit the Enum kotlin.resolve.constants.evaluateCompileTimeType which is all uppercase and doesn't have the concept of
// nullability.
private fun String.toCompilTimeTypeFormat(): String {
    return this.uppercase().removeSuffix("?")
}

private val KotlinType.typeName: String
    get(): String = constructor.declarationDescriptor!!.name.asString()
