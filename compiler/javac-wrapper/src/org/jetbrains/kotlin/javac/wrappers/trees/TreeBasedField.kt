/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.javac.wrappers.trees

import com.sun.source.util.TreePath
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.Name

class TreeBasedField(
        tree: JCTree.JCVariableDecl,
        treePath: TreePath,
        containingClass: JavaClass,
        javac: JavacWrapper
) : TreeBasedMember<JCTree.JCVariableDecl>(tree, treePath, containingClass, javac), JavaField {

    override val name: Name
        get() = Name.identifier(tree.name.toString())

    override val isAbstract: Boolean
        get() = tree.modifiers.isAbstract

    override val isStatic: Boolean
        get() = containingClass.isInterface || tree.modifiers.isStatic

    override val isFinal: Boolean
        get() = containingClass.isInterface || tree.modifiers.isFinal

    override val visibility: Visibility
        get() = if (containingClass.isInterface) Visibilities.PUBLIC else tree.modifiers.visibility

    override val isEnumEntry: Boolean
        get() = tree.modifiers.flags and Flags.ENUM.toLong() != 0L

    override val type: JavaType
        get() = TreeBasedType.create(tree.getType(), treePath, javac)

    override val initializerValue: Any?
        get() = tree.init?.let { initExpr ->
            if (hasConstantNotNullInitializer) {
                when (initExpr) {
                    is JCTree.JCLiteral -> initExpr.value
                    is JCTree.JCIdent -> containingClass.fields
                            .find { it.name == initExpr.name.toString().let { Name.identifier(it) } }
                            ?.initializerValue
                    is JCTree.JCBinary -> binaryInitializerValue(initExpr)
                    is JCTree.JCFieldAccess -> fieldAccessValue(initExpr)
                    else -> null
                }
            }
            else {
                null
            }
        }

    override val hasConstantNotNullInitializer: Boolean
        get() = tree.init?.let {
            val type = this.type

            isFinal && ((type is TreeBasedPrimitiveType) ||
                        (type is TreeBasedNonGenericClassifierType &&
                         type.classifierQualifiedName == "java.lang.String"))
        } ?: false

    private fun fieldAccessValue(value: JCTree.JCFieldAccess): Any? {
        val newTreePath = javac.getTreePath(value.selected, treePath.compilationUnit)
        val javaClass = javac.resolve(newTreePath) as? JavaClass ?: return null
        val fieldName = value.name.toString().let { Name.identifier(it) }

        return javaClass.fields
                .find { it.name == fieldName }
                ?.initializerValue
    }

    private fun binaryInitializerValue(value: JCTree.JCBinary): Any? {
        val lhs = value.lhs
        val rhs = value.rhs
        val opcode = value.tag

        fun getValue(expr: JCTree.JCExpression): Any? {
            return when (expr) {
                is JCTree.JCFieldAccess -> fieldAccessValue(expr)
                is JCTree.JCLiteral -> expr.value
                is JCTree.JCBinary -> binaryInitializerValue(expr)
                else -> null
            }
        }

        fun getExpressionType(expression: Int): Any? {
            val type = type as? JavaPrimitiveType ?: return null
            return when (type.type) {
                PrimitiveType.DOUBLE -> expression.toDouble()
                PrimitiveType.INT -> expression
                PrimitiveType.FLOAT -> expression.toFloat()
                PrimitiveType.LONG -> expression.toLong()
                PrimitiveType.BYTE -> expression.toByte()
                else -> null
            }
        }

        fun calculate(lhsValue: Any?, rhsValue: Any?): Any? {

            val l = (lhsValue as? Number)?.toInt() ?: return null
            val r = (rhsValue as? Number)?.toInt() ?: return null
            return when (opcode) {
                JCTree.Tag.PLUS -> getExpressionType(l + r)
                JCTree.Tag.MINUS -> getExpressionType(l - r)
                JCTree.Tag.MUL -> getExpressionType(l * r)
                JCTree.Tag.DIV -> getExpressionType(l / r)
                else -> null
            }
        }

        val lhsValue = getValue(lhs) ?: return null
        val rhsValue = getValue(rhs) ?: return null

        return calculate(lhsValue, rhsValue)
    }

}