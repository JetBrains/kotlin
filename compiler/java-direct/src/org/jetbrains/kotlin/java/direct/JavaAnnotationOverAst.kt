/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val JavaSyntaxNode.nodeType: String get() = type.toString()

class JavaAnnotationOverAst(
    node: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val parameterList = node.findChildByType("ANNOTATION_PARAMETER_LIST")
            if (parameterList == null) return emptyList()

            return parameterList.getChildrenByType("NAME_VALUE_PAIR").map { nvp ->
                createAnnotationArgument(nvp, resolutionContext)
            }
        }

    /**
     * The simple or qualified name of the annotation as it appears in source.
     * For `@Deprecated`, returns "Deprecated".
     * For `@java.lang.Deprecated`, returns "java.lang.Deprecated".
     */
    private val annotationName: String?
        get() = node.findChildByType("JAVA_CODE_REFERENCE")?.text

    override val classId: ClassId?
        get() {
            val reference = annotationName ?: return null

            // If already qualified (contains dot), use as-is
            if (reference.contains('.')) {
                return ClassId.topLevel(FqName(reference))
            }

            // Try to resolve via explicit imports
            val imported = resolutionContext.getSimpleImport(reference)
            if (imported != null) {
                return ClassId.topLevel(imported)
            }

            // Return unqualified - FIR will need to resolve via resolveAnnotation
            return ClassId.topLevel(FqName(reference))
        }

    /**
     * Whether this annotation is already resolved.
     * Returns false when the annotation name is unqualified and not explicitly imported.
     */
    override val isResolved: Boolean
        get() {
            val reference = annotationName ?: return true
            // Resolved if fully qualified or explicitly imported
            return reference.contains('.') || resolutionContext.getSimpleImport(reference) != null
        }

    /**
     * Resolves this annotation's class using the provided callback.
     * Uses the same resolution logic as types: same package, java.lang, star imports.
     */
    override fun resolveAnnotation(tryResolve: (String) -> Boolean): String? {
        val reference = annotationName ?: return null

        // If already qualified, return as-is (but verify it exists)
        if (reference.contains('.')) {
            return if (tryResolve(reference)) reference else null
        }

        // Try to resolve via explicit imports
        val imported = resolutionContext.getSimpleImport(reference)
        if (imported != null) {
            return imported.asString()
        }

        // Use the same resolution logic as types: same package, java.lang, star imports
        return resolutionContext.resolveWithCallback(reference, tryResolve)
    }

    override fun resolve(): JavaClass? = null
}

private fun createAnnotationArgument(
    nameValuePair: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    val name = nameValuePair.findChildByType("IDENTIFIER")?.let { Name.identifier(it.text) }

    // Find the value expression - it's the child that's not IDENTIFIER, EQ, or whitespace
    val valueNode = nameValuePair.children.firstOrNull { child ->
        child.nodeType !in listOf("IDENTIFIER", "EQ", "WHITE_SPACE")
    }

    return createAnnotationArgumentFromValue(name, valueNode, resolutionContext)
}

internal fun createAnnotationArgumentFromValue(
    name: Name?,
    valueNode: JavaSyntaxNode?,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    if (valueNode == null) {
        return JavaUnknownAnnotationArgumentOverAst(name)
    }

    return when (valueNode.nodeType) {
        "LITERAL_EXPRESSION" -> {
            val value = evaluateLiteral(valueNode)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        "ARRAY_INITIALIZER_EXPRESSION", "ANNOTATION_ARRAY_INITIALIZER" -> {
            JavaArrayAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        "REFERENCE_EXPRESSION" -> {
            // Could be enum constant reference (e.g., RetentionPolicy.RUNTIME)
            JavaEnumValueAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        "CLASS_OBJECT_ACCESS_EXPRESSION" -> {
            JavaClassObjectAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        "ANNOTATION" -> {
            JavaAnnotationAsAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        "PREFIX_EXPRESSION", "BINARY_EXPRESSION" -> {
            // Constant expressions like -1 or 1 + 2
            val value = evaluateConstantExpression(valueNode)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        else -> {
            JavaUnknownAnnotationArgumentOverAst(name)
        }
    }
}

private fun evaluateLiteral(node: JavaSyntaxNode): Any? {
    val literalChild = node.children.firstOrNull() ?: return null
    val text = literalChild.text

    return when (literalChild.nodeType) {
        "STRING_LITERAL" -> {
            // Remove surrounding quotes and unescape
            if (text.length >= 2) {
                text.substring(1, text.length - 1).unescapeJavaString()
            } else text
        }
        "CHARACTER_LITERAL" -> {
            if (text.length >= 3) {
                text.substring(1, text.length - 1).unescapeJavaString().firstOrNull()
            } else null
        }
        "TRUE_KEYWORD" -> true
        "FALSE_KEYWORD" -> false
        "NULL_LITERAL" -> null
        "INTEGER_LITERAL" -> parseIntegerLiteral(text)
        "LONG_LITERAL" -> parseLongLiteral(text)
        "FLOAT_LITERAL" -> parseFloatLiteral(text)
        "DOUBLE_LITERAL" -> parseDoubleLiteral(text)
        else -> {
            // Fallback: try to parse as number
            text.toIntOrNull() ?: text.toLongOrNull() ?: text.toDoubleOrNull() ?: text
        }
    }
}

private fun evaluateConstantExpression(node: JavaSyntaxNode): Any? {
    when (node.nodeType) {
        "PREFIX_EXPRESSION" -> {
            val operator = node.children.firstOrNull()?.nodeType
            val operand = node.children.getOrNull(1)
            if (operator == "MINUS" && operand != null) {
                val value = if (operand.nodeType == "LITERAL_EXPRESSION") {
                    evaluateLiteral(operand)
                } else {
                    evaluateConstantExpression(operand)
                }
                return when (value) {
                    is Int -> -value
                    is Long -> -value
                    is Float -> -value
                    is Double -> -value
                    else -> null
                }
            }
        }
        "LITERAL_EXPRESSION" -> return evaluateLiteral(node)
    }
    return null
}

private fun parseIntegerLiteral(text: String): Any {
    val cleaned = text.replace("_", "")
    return when {
        cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
            cleaned.substring(2).toIntOrNull(16) ?: cleaned.substring(2).toLongOrNull(16) ?: 0
        cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
            cleaned.substring(2).toIntOrNull(2) ?: cleaned.substring(2).toLongOrNull(2) ?: 0
        cleaned.startsWith("0") && cleaned.length > 1 ->
            cleaned.toIntOrNull(8) ?: cleaned.toLongOrNull(8) ?: 0
        else -> cleaned.toIntOrNull() ?: cleaned.toLongOrNull() ?: 0
    }
}

private fun parseLongLiteral(text: String): Long {
    val cleaned = text.replace("_", "").removeSuffix("L").removeSuffix("l")
    return when {
        cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
            cleaned.substring(2).toLongOrNull(16) ?: 0L
        cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
            cleaned.substring(2).toLongOrNull(2) ?: 0L
        cleaned.startsWith("0") && cleaned.length > 1 ->
            cleaned.toLongOrNull(8) ?: 0L
        else -> cleaned.toLongOrNull() ?: 0L
    }
}

private fun parseFloatLiteral(text: String): Float {
    val cleaned = text.replace("_", "").removeSuffix("F").removeSuffix("f")
    return cleaned.toFloatOrNull() ?: 0f
}

private fun parseDoubleLiteral(text: String): Double {
    val cleaned = text.replace("_", "").removeSuffix("D").removeSuffix("d")
    return cleaned.toDoubleOrNull() ?: 0.0
}

private fun String.unescapeJavaString(): String {
    val sb = StringBuilder()
    var i = 0
    while (i < length) {
        if (this[i] == '\\' && i + 1 < length) {
            when (this[i + 1]) {
                'n' -> { sb.append('\n'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                'b' -> { sb.append('\b'); i += 2 }
                'f' -> { sb.append('\u000C'); i += 2 }
                '\'' -> { sb.append('\''); i += 2 }
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                'u' -> {
                    if (i + 5 < length) {
                        val hex = substring(i + 2, i + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            sb.append(code.toChar())
                            i += 6
                        } else {
                            sb.append(this[i])
                            i++
                        }
                    } else {
                        sb.append(this[i])
                        i++
                    }
                }
                in '0'..'7' -> {
                    // Octal escape
                    var end = i + 2
                    while (end < length && end < i + 4 && this[end] in '0'..'7') end++
                    val octal = substring(i + 1, end)
                    val code = octal.toIntOrNull(8)
                    if (code != null) {
                        sb.append(code.toChar())
                        i = end
                    } else {
                        sb.append(this[i])
                        i++
                    }
                }
                else -> { sb.append(this[i]); i++ }
            }
        } else {
            sb.append(this[i])
            i++
        }
    }
    return sb.toString()
}

class JavaLiteralAnnotationArgumentOverAst(
    override val name: Name?,
    override val value: Any?,
) : JavaLiteralAnnotationArgument

class JavaArrayAnnotationArgumentOverAst(
    override val name: Name?,
    private val arrayNode: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaArrayAnnotationArgument {
    override fun getElements(): List<JavaAnnotationArgument> {
        return arrayNode.children
            .filter { it.nodeType !in listOf("LBRACE", "RBRACE", "COMMA", "WHITE_SPACE") }
            .map { createAnnotationArgumentFromValue(null, it, resolutionContext) }
    }
}

class JavaEnumValueAnnotationArgumentOverAst(
    override val name: Name?,
    private val refNode: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaEnumValueAnnotationArgument {
    override val enumClassId: ClassId?
        get() {
            val text = refNode.text
            val lastDot = text.lastIndexOf('.')
            if (lastDot < 0) return null
            val className = text.substring(0, lastDot)

            // If already qualified (contains dot), use as-is
            if (className.contains('.')) {
                return ClassId.topLevel(FqName(className))
            }

            // Try to resolve via explicit imports first
            val imported = resolutionContext.getSimpleImport(className)
            if (imported != null) {
                return ClassId.topLevel(imported)
            }

            // Assume same package - FIR will verify during resolution
            val packageFqName = resolutionContext.packageFqName
            return if (packageFqName.isRoot) {
                ClassId.topLevel(FqName(className))
            } else {
                ClassId.topLevel(FqName("${packageFqName.asString()}.$className"))
            }
        }

    override val entryName: Name?
        get() {
            val text = refNode.text
            val lastDot = text.lastIndexOf('.')
            return if (lastDot >= 0) Name.identifier(text.substring(lastDot + 1))
            else Name.identifier(text)
        }
}

class JavaClassObjectAnnotationArgumentOverAst(
    override val name: Name?,
    private val classObjNode: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaClassObjectAnnotationArgument {
    override fun getReferencedType(): JavaType {
        // CLASS_OBJECT_ACCESS_EXPRESSION typically has structure: TYPE.class
        // Find the type reference before .class
        val typeNode = classObjNode.findChildByType("TYPE")
            ?: classObjNode.findChildByType("JAVA_CODE_REFERENCE")

        return if (typeNode != null) {
            createJavaType(typeNode, resolutionContext)
        } else {
            // Fallback: create a classifier type from the node itself
            JavaClassifierTypeOverAst(classObjNode, resolutionContext)
        }
    }
}

class JavaAnnotationAsAnnotationArgumentOverAst(
    override val name: Name?,
    private val annotationNode: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation(): JavaAnnotation {
        return JavaAnnotationOverAst(annotationNode, resolutionContext)
    }
}

class JavaUnknownAnnotationArgumentOverAst(
    override val name: Name?,
) : JavaUnknownAnnotationArgument
