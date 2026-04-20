/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JavaAnnotationOverAst(
    node: JavaLightNode,
    tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaElementOverAst(node, tree), JavaAnnotation {
    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val parameterList = tree.findChildByType(node, JavaSyntaxElementType.ANNOTATION_PARAMETER_LIST) ?: return emptyList()
            return tree.getChildrenByType(parameterList, JavaSyntaxElementType.NAME_VALUE_PAIR).map { nvp ->
                createAnnotationArgument(nvp, tree, resolutionContext)
            }
        }

    /**
     * The simple or qualified name of the annotation as it appears in source.
     * For `@Deprecated`, returns "Deprecated".
     * For `@java.lang.Deprecated`, returns "java.lang.Deprecated".
     */
    private val annotationName: String?
        get() = tree.findChildByType(node, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { tree.getText(it).toString() }

    override val classId: ClassId?
        get() {
            val reference = annotationName ?: return null

            if (reference.contains('.')) {
                return ClassId.topLevel(FqName(reference))
            }

            val imported = resolutionContext.getSimpleImport(reference)
            if (imported != null) {
                return ClassId.topLevel(imported)
            }

            // Return unqualified - FIR will need to resolve via resolveAnnotation
            return ClassId.topLevel(FqName(reference))
        }

    override val isResolved: Boolean
        get() {
            val reference = annotationName ?: return true
            return reference.contains('.') || resolutionContext.getSimpleImport(reference) != null
        }

    override fun resolveAnnotation(tryResolve: (ClassId) -> Boolean): ClassId? {
        val reference = annotationName ?: return null
        return resolutionContext.resolve(reference, tryResolve)
    }

    override fun resolve(): JavaClass? = null
}

private fun createAnnotationArgument(
    nameValuePair: JavaLightNode,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    val name = tree.findChildByType(nameValuePair, JavaSyntaxTokenType.IDENTIFIER)?.let {
        Name.identifier(tree.getText(it).toString())
    }

    val valueNode = tree.getChildren(nameValuePair).firstOrNull { child ->
        val t = tree.getType(child)
        t != JavaSyntaxTokenType.IDENTIFIER && t != JavaSyntaxTokenType.EQ && t != SyntaxTokenTypes.WHITE_SPACE
    }

    return createAnnotationArgumentFromValue(name, valueNode, tree, resolutionContext)
}

internal fun createAnnotationArgumentFromValue(
    name: Name?,
    valueNode: JavaLightNode?,
    tree: JavaLightTree,
    resolutionContext: JavaResolutionContext,
): JavaAnnotationArgument {
    if (valueNode == null) {
        return JavaUnknownAnnotationArgumentOverAst(name)
    }

    return when (tree.getType(valueNode)) {
        JavaSyntaxElementType.LITERAL_EXPRESSION -> {
            val value = evaluateLiteral(valueNode, tree)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        JavaSyntaxElementType.ARRAY_INITIALIZER_EXPRESSION, JavaSyntaxElementType.ANNOTATION_ARRAY_INITIALIZER -> {
            JavaArrayAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.REFERENCE_EXPRESSION -> {
            // Could be enum constant reference (e.g., RetentionPolicy.RUNTIME)
            // or constant field reference (e.g., KotlinClass.FOO_INT)
            // FIR will determine which it is during resolution
            JavaEnumValueAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.CLASS_OBJECT_ACCESS_EXPRESSION -> {
            JavaClassObjectAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.ANNOTATION -> {
            JavaAnnotationAsAnnotationArgumentOverAst(name, valueNode, tree, resolutionContext)
        }
        JavaSyntaxElementType.PREFIX_EXPRESSION, JavaSyntaxElementType.BINARY_EXPRESSION -> {
            val value = evaluateConstantExpression(valueNode, tree)
            JavaLiteralAnnotationArgumentOverAst(name, value)
        }
        else -> {
            JavaUnknownAnnotationArgumentOverAst(name)
        }
    }
}

private fun evaluateLiteral(node: JavaLightNode, tree: JavaLightTree): Any? {
    val literalChild = tree.getChildren(node).firstOrNull() ?: return null
    val text = tree.getText(literalChild).toString()

    return when (tree.getType(literalChild)) {
        JavaSyntaxTokenType.STRING_LITERAL -> {
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
            // TODO: check against specs
            text.toIntOrNull() ?: text.toLongOrNull() ?: text.toDoubleOrNull() ?: text
        }
    }
}

private val ANNOTATION_BINARY_OPERATOR_TYPES = setOf(
    JavaSyntaxTokenType.PLUS, JavaSyntaxTokenType.MINUS, JavaSyntaxTokenType.ASTERISK,
    JavaSyntaxTokenType.DIV, JavaSyntaxTokenType.PERC
)

// TODO: check if it needs to be replaced with ConstantEvaluator
private fun evaluateConstantExpression(node: JavaLightNode, tree: JavaLightTree): Any? {
    when (tree.getType(node)) {
        JavaSyntaxElementType.PREFIX_EXPRESSION -> {
            val children = tree.getChildren(node)
            val firstChild = children.firstOrNull()
            val operand = children.getOrNull(1)
            if (firstChild != null && tree.getType(firstChild) == JavaSyntaxTokenType.MINUS && operand != null) {
                val value = if (tree.getType(operand) == JavaSyntaxElementType.LITERAL_EXPRESSION) {
                    evaluateLiteral(operand, tree)
                } else {
                    evaluateConstantExpression(operand, tree)
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
            val allChildren = tree.getChildren(node)
            val operands = allChildren.filter {
                val t = tree.getType(it)
                t != SyntaxTokenTypes.WHITE_SPACE && t !in ANNOTATION_BINARY_OPERATOR_TYPES
            }
            val operatorNode = allChildren.firstOrNull { tree.getType(it) in ANNOTATION_BINARY_OPERATOR_TYPES }
            if (operands.size == 2 && operatorNode != null) {
                val left = evaluateConstantExpression(operands[0], tree)
                val right = evaluateConstantExpression(operands[1], tree)
                return evaluateBinaryExpression(left, tree.getType(operatorNode), right)
            }
        }
        JavaSyntaxElementType.LITERAL_EXPRESSION -> return evaluateLiteral(node, tree)
    }
    return null
}

private fun evaluateBinaryExpression(left: Any?, operator: com.intellij.platform.syntax.SyntaxElementType, right: Any?): Any? {
    if (left == null || right == null) return null
    if (operator == JavaSyntaxTokenType.PLUS && (left is String || right is String)) {
        return left.toString() + right.toString()
    }
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
    return if (left is Long || right is Long) result else result.toInt()
}


class JavaLiteralAnnotationArgumentOverAst(
    override val name: Name?,
    override val value: Any?,
) : JavaLiteralAnnotationArgument

class JavaArrayAnnotationArgumentOverAst(
    override val name: Name?,
    private val arrayNode: JavaLightNode,
    private val tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaArrayAnnotationArgument {
    override fun getElements(): List<JavaAnnotationArgument> {
        return tree.getChildren(arrayNode)
            .filter {
                val t = tree.getType(it)
                t != JavaSyntaxTokenType.LBRACE && t != JavaSyntaxTokenType.RBRACE &&
                        t != JavaSyntaxTokenType.COMMA && t != SyntaxTokenTypes.WHITE_SPACE
            }
            .map { createAnnotationArgumentFromValue(null, it, tree, resolutionContext) }
    }
}

class JavaEnumValueAnnotationArgumentOverAst(
    override val name: Name?,
    private val refNode: JavaLightNode,
    private val tree: JavaLightTree,
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
        val text = tree.getText(refNode).toString()
        if (text.contains('.')) return@lazy null
        val importedFqn = resolutionContext.getSimpleImport(text) ?: return@lazy null
        val fqnStr = importedFqn.asString()
        val lastDot = fqnStr.lastIndexOf('.')
        if (lastDot < 0) return@lazy null
        fqnStr.substring(0, lastDot) to fqnStr.substring(lastDot + 1)
    }

    private val className: String?
        get() {
            val text = tree.getText(refNode).toString()
            val lastDot = text.lastIndexOf('.')
            if (lastDot >= 0) return text.substring(0, lastDot)
            return staticImportResolution?.first
        }

    override val isResolved: Boolean
        get() {
            val name = className ?: return true
            if (resolutionContext.getSimpleImport(name) != null) return true
            if (staticImportResolution != null) return false
            return false
        }

    override val enumClassId: ClassId?
        get() {
            val className = className

            if (className == null) {
                return null
            }

            val imported = resolutionContext.getSimpleImport(className)
            if (imported != null) {
                return ClassId.topLevel(imported)
            }

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
            val text = tree.getText(refNode).toString()
            val lastDot = text.lastIndexOf('.')
            if (lastDot >= 0) return Name.identifier(text.substring(lastDot + 1))
            staticImportResolution?.let { return Name.identifier(it.second) }
            return Name.identifier(text)
        }
}

class JavaClassObjectAnnotationArgumentOverAst(
    override val name: Name?,
    private val classObjNode: JavaLightNode,
    private val tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaClassObjectAnnotationArgument {
    override fun getReferencedType(): JavaType {
        val typeNode = tree.findChildByType(classObjNode, JavaSyntaxElementType.TYPE)
            ?: tree.findChildByType(classObjNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE)

        return if (typeNode != null) {
            createJavaType(typeNode, tree, resolutionContext)
        } else {
            JavaClassifierTypeOverAst(classObjNode, tree, resolutionContext)
        }
    }
}

class JavaAnnotationAsAnnotationArgumentOverAst(
    override val name: Name?,
    private val annotationNode: JavaLightNode,
    private val tree: JavaLightTree,
    private val resolutionContext: JavaResolutionContext,
) : JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation(): JavaAnnotation {
        return JavaAnnotationOverAst(annotationNode, tree, resolutionContext)
    }
}

class JavaUnknownAnnotationArgumentOverAst(
    override val name: Name?,
) : JavaUnknownAnnotationArgument
