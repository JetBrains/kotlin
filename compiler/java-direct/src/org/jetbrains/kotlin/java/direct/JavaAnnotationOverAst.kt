/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaAnnotationOverAst(
    node: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node), JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val parameterList = node.findChildByType(JavaSyntaxElementType.ANNOTATION_PARAMETER_LIST) ?: return emptyList()
            return parameterList.getChildrenByType(JavaSyntaxElementType.NAME_VALUE_PAIR).map { nvp ->
                createAnnotationArgument(nvp, resolutionContext)
            }
        }

    /**
     * The simple or qualified name of the annotation as it appears in source.
     * For `@Deprecated`, returns "Deprecated".
     * For `@java.lang.Deprecated`, returns "java.lang.Deprecated".
     */
    private val annotationName: String?
        get() = node.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.text

    override val classId: ClassId?
        get() {
            val reference = annotationName ?: return null

            // If already qualified (contains dot), parse as qualified name
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
    override fun resolveAnnotation(tryResolve: (ClassId) -> Boolean): ClassId? {
        val reference = annotationName ?: return null
        return resolutionContext.resolve(reference, tryResolve)
    }

    override fun resolve(): JavaClass? = null
}

private fun createAnnotationArgument(
    nameValuePair: JavaSyntaxNode,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    val name = nameValuePair.findChildByType(JavaSyntaxTokenType.IDENTIFIER)?.let { Name.identifier(it.text) }

    // Find the value expression - it's the child that's not IDENTIFIER, EQ, or whitespace
    val valueNode = nameValuePair.children.firstOrNull { child ->
        child.type != JavaSyntaxTokenType.IDENTIFIER && child.type != JavaSyntaxTokenType.EQ && child.type != SyntaxTokenTypes.WHITE_SPACE
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

    return when (valueNode.type) {
        JavaSyntaxElementType.LITERAL_EXPRESSION -> {
            val value = evaluateLiteral(valueNode)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        JavaSyntaxElementType.ARRAY_INITIALIZER_EXPRESSION, JavaSyntaxElementType.ANNOTATION_ARRAY_INITIALIZER -> {
            JavaArrayAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        JavaSyntaxElementType.REFERENCE_EXPRESSION -> {
            // Could be enum constant reference (e.g., RetentionPolicy.RUNTIME)
            // or constant field reference (e.g., KotlinClass.FOO_INT)
            // FIR will determine which it is during resolution
            JavaEnumValueAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        JavaSyntaxElementType.CLASS_OBJECT_ACCESS_EXPRESSION -> {
            JavaClassObjectAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        JavaSyntaxElementType.ANNOTATION -> {
            JavaAnnotationAsAnnotationArgumentOverAst(name, valueNode, resolutionContext)
        }
        JavaSyntaxElementType.PREFIX_EXPRESSION, JavaSyntaxElementType.BINARY_EXPRESSION -> {
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

    return when (literalChild.type) {
        JavaSyntaxTokenType.STRING_LITERAL -> {
            // Remove surrounding quotes and unescape
            if (text.length >= 2) {
                JavaLiteralParser.unescapeJavaString(text.substring(1, text.length - 1))
            } else text
        }
        JavaSyntaxTokenType.CHARACTER_LITERAL -> {
            if (text.length >= 3) {
                JavaLiteralParser.unescapeJavaString(text.substring(1, text.length - 1)).firstOrNull()
            } else null
        }
        JavaSyntaxTokenType.TRUE_KEYWORD -> true
        JavaSyntaxTokenType.FALSE_KEYWORD -> false
        JavaSyntaxTokenType.NULL_KEYWORD -> null
        JavaSyntaxTokenType.INTEGER_LITERAL -> JavaLiteralParser.parseIntegerLiteral(text)
        JavaSyntaxTokenType.LONG_LITERAL -> JavaLiteralParser.parseLongLiteral(text)
        JavaSyntaxTokenType.FLOAT_LITERAL -> JavaLiteralParser.parseFloatLiteral(text)
        JavaSyntaxTokenType.DOUBLE_LITERAL -> JavaLiteralParser.parseDoubleLiteral(text)
        else -> {
            // Fallback: try to parse as number
            text.toIntOrNull() ?: text.toLongOrNull() ?: text.toDoubleOrNull() ?: text
        }
    }
}

private val ANNOTATION_BINARY_OPERATOR_TYPES = setOf(
    JavaSyntaxTokenType.PLUS, JavaSyntaxTokenType.MINUS, JavaSyntaxTokenType.ASTERISK,
    JavaSyntaxTokenType.DIV, JavaSyntaxTokenType.PERC
)

private fun evaluateConstantExpression(node: JavaSyntaxNode): Any? {
    when (node.type) {
        JavaSyntaxElementType.PREFIX_EXPRESSION -> {
            val firstChild = node.children.firstOrNull()
            val operand = node.children.getOrNull(1)
            if (firstChild?.type == JavaSyntaxTokenType.MINUS && operand != null) {
                val value = if (operand.type == JavaSyntaxElementType.LITERAL_EXPRESSION) {
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
        JavaSyntaxElementType.BINARY_EXPRESSION -> {
            val operands = node.children.filter {
                it.type != SyntaxTokenTypes.WHITE_SPACE && it.type !in ANNOTATION_BINARY_OPERATOR_TYPES
            }
            val operatorNode = node.children.firstOrNull {
                it.type in ANNOTATION_BINARY_OPERATOR_TYPES
            }
            if (operands.size == 2 && operatorNode != null) {
                val left = evaluateConstantExpression(operands[0])
                val right = evaluateConstantExpression(operands[1])
                return evaluateBinaryExpression(left, operatorNode.type, right)
            }
        }
        JavaSyntaxElementType.LITERAL_EXPRESSION -> return evaluateLiteral(node)
    }
    return null
}

private fun evaluateBinaryExpression(left: Any?, operator: com.intellij.platform.syntax.SyntaxElementType, right: Any?): Any? {
    if (left == null || right == null) return null
    // String concatenation
    if (operator == JavaSyntaxTokenType.PLUS && (left is String || right is String)) {
        return left.toString() + right.toString()
    }
    // Numeric operations
    return when (operator) {
        JavaSyntaxTokenType.PLUS -> numericBinaryOp(left, right) { a, b -> a + b }
        JavaSyntaxTokenType.MINUS -> numericBinaryOp(left, right) { a, b -> a - b }
        JavaSyntaxTokenType.ASTERISK -> numericBinaryOp(left, right) { a, b -> a * b }
        JavaSyntaxTokenType.DIV -> numericBinaryOp(left, right) { a, b -> if (b != 0L) a / b else 0L }
        JavaSyntaxTokenType.PERC -> numericBinaryOp(left, right) { a, b -> if (b != 0L) a % b else 0L }
        else -> null
    }
}

private fun numericBinaryOp(left: Any, right: Any, op: (Long, Long) -> Long): Any? {
    val l = (left as? Number)?.toLong() ?: return null
    val r = (right as? Number)?.toLong() ?: return null
    val result = op(l, r)
    // If either operand was Long, keep Long; otherwise try Int
    return if (left is Long || right is Long) result else result.toInt()
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
            .filter { it.type != JavaSyntaxTokenType.LBRACE && it.type != JavaSyntaxTokenType.RBRACE && it.type != JavaSyntaxTokenType.COMMA && it.type != SyntaxTokenTypes.WHITE_SPACE }
            .map { createAnnotationArgumentFromValue(null, it, resolutionContext) }
    }
}

class JavaEnumValueAnnotationArgumentOverAst(
    override val name: Name?,
    private val refNode: JavaSyntaxNode,
    private val resolutionContext: JavaResolutionContext,
) : JavaEnumValueAnnotationArgument {

    /**
     * For bare identifiers (no dots), tries to resolve via static imports.
     * E.g., `import static example.KotlinDtoMapping.ID` makes `ID` resolvable
     * as className="example.KotlinDtoMapping", entryName="ID".
     *
     * Returns a pair of (className, memberName) if the static import is found, null otherwise.
     */
    private val staticImportResolution: Pair<String, String>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val text = refNode.text
        if (text.contains('.')) return@lazy null // Not a bare identifier
        val importedFqn = resolutionContext.getSimpleImport(text) ?: return@lazy null
        val fqnStr = importedFqn.asString()
        val lastDot = fqnStr.lastIndexOf('.')
        if (lastDot < 0) return@lazy null
        // Split: "example.KotlinDtoMapping.ID" → class="example.KotlinDtoMapping", member="ID"
        fqnStr.substring(0, lastDot) to fqnStr.substring(lastDot + 1)
    }

    /**
     * The class name part of the enum reference (everything before the last dot).
     * For "NLS.Capitalization.NotSpecified", returns "NLS.Capitalization".
     * For bare identifiers resolved via static import, returns the class qualifier.
     */
    private val className: String?
        get() {
            val text = refNode.text
            val lastDot = text.lastIndexOf('.')
            if (lastDot >= 0) return text.substring(0, lastDot)
            // Bare identifier — try static import resolution
            return staticImportResolution?.first
        }

    override val isResolved: Boolean
        get() {
            val name = className ?: return true
            // Resolved if explicitly imported
            if (resolutionContext.getSimpleImport(name) != null) return true
            // Resolved if resolved via static import
            if (staticImportResolution != null) return false // class part still needs resolution
            // Not resolved if it contains dots (could be nested class)
            // and is not a simple name
            return false
        }

    override val enumClassId: ClassId?
        get() {
            val className = className

            if (className == null) {
                // Bare identifier (no dots) and no static import found.
                // We can't resolve enumClassId for bare names without lazy class lookup (which is
                // forbidden at this phase). Return null so FIR falls back to expected type;
                // the backend will skip unresolvable error expressions in annotations (AnnotationCodegen).
                return null
            }

            // Try to resolve via explicit imports first
            val imported = resolutionContext.getSimpleImport(className)
            if (imported != null) {
                return ClassId.topLevel(imported)
            }

            // For unresolved references, return a best-effort ClassId
            // FIR should use resolveEnumClass for proper resolution
            val packageFqName = resolutionContext.packageFqName
            return if (packageFqName.isRoot) {
                ClassId.topLevel(FqName(className))
            } else {
                ClassId.topLevel(FqName("${packageFqName.asString()}.$className"))
            }
        }

    override fun resolveEnumClass(tryResolve: (ClassId) -> Boolean): ClassId? {
        val className = className ?: return null
        return resolutionContext.resolve(className, tryResolve)
    }

    override val entryName: Name?
        get() {
            val text = refNode.text
            val lastDot = text.lastIndexOf('.')
            if (lastDot >= 0) return Name.identifier(text.substring(lastDot + 1))
            // Bare identifier — if resolved via static import, return the member name
            staticImportResolution?.let { return Name.identifier(it.second) }
            return Name.identifier(text)
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
        val typeNode = classObjNode.findChildByType(JavaSyntaxElementType.TYPE)
            ?: classObjNode.findChildByType(JavaSyntaxElementType.JAVA_CODE_REFERENCE)

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

