/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.ReadValueInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.cfg.variable.PseudocodeVariablesData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.types.expressions.OperatorConventions

interface LeakingThisDescriptor {
    val classOrObject: KtClassOrObject

    data class PropertyIsNull(val property: PropertyDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor

    data class NonFinalClass(val klass: ClassDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor

    data class NonFinalProperty(val property: PropertyDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor

    data class NonFinalFunction(val function: FunctionDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor
}

class ConstructorConsistencyChecker private constructor(
    private val classOrObject: KtClassOrObject,
    private val classDescriptor: ClassDescriptor,
    private val trace: BindingTrace,
    private val pseudocode: Pseudocode,
    private val variablesData: PseudocodeVariablesData
) {
    private val finalClass = classDescriptor.isFinalClass

    private fun insideLValue(reference: KtReferenceExpression): Boolean {
        val binary = reference.getStrictParentOfType<KtBinaryExpression>() ?: return false
        if (binary.operationToken in KtTokens.ALL_ASSIGNMENTS) {
            val binaryLeft = binary.left
            var current: PsiElement = reference
            while (current !== binaryLeft && current !== binary) {
                current = current.parent ?: return false
            }
            return current === binaryLeft
        }
        return false
    }

    private fun safeReferenceUsage(reference: KtReferenceExpression): Boolean {
        val descriptor = trace.get(BindingContext.REFERENCE_TARGET, reference)
        if (descriptor is PropertyDescriptor) {
            if (!finalClass && descriptor.isOverridable) {
                trace.record(BindingContext.LEAKING_THIS, reference, LeakingThisDescriptor.NonFinalProperty(descriptor, classOrObject))
                return true
            }
            if (descriptor.containingDeclaration != classDescriptor) return true
            return if (insideLValue(reference)) descriptor.setter?.isDefault != false else descriptor.getter?.isDefault != false
        }
        return true
    }

    private fun safeThisUsage(expression: KtThisExpression): Boolean {
        val referenceDescriptor = trace.get(BindingContext.REFERENCE_TARGET, expression.instanceReference)
        if (referenceDescriptor != classDescriptor) return true
        val parent = expression.parent
        return when (parent) {
            is KtQualifiedExpression -> (parent.selectorExpression as? KtSimpleNameExpression)?.let { safeReferenceUsage(it) } ?: false
            is KtBinaryExpression -> OperatorConventions.IDENTITY_EQUALS_OPERATIONS.contains(parent.operationToken)
            else -> false
        }
    }

    private fun safeCallUsage(expression: KtCallExpression): Boolean {
        val callee = expression.calleeExpression
        if (callee is KtReferenceExpression) {
            val descriptor = trace.get(BindingContext.REFERENCE_TARGET, callee)
            if (descriptor is FunctionDescriptor) {
                val containingDescriptor = descriptor.containingDeclaration
                if (containingDescriptor != classDescriptor) return true
                if (!finalClass && descriptor.isOverridable) {
                    trace.record(BindingContext.LEAKING_THIS, callee, LeakingThisDescriptor.NonFinalFunction(descriptor, classOrObject))
                    return true
                }
            }
        }
        return false
    }

    fun check() {
        // List of properties to initialize
        val propertyDescriptors = variablesData.getDeclaredVariables(pseudocode, false)
            .filterIsInstance<PropertyDescriptor>()
            .filter { trace.get(BindingContext.BACKING_FIELD_REQUIRED, it) == true }
        pseudocode.traverse(
            TraversalOrder.FORWARD, variablesData.variableInitializers
        ) { instruction, enterData, _ ->

            fun firstUninitializedNotNullProperty() = propertyDescriptors.firstOrNull {
                !it.type.isMarkedNullable && !KotlinBuiltIns.isPrimitiveType(it.type) &&
                        !it.isLateInit && !(enterData.getOrNull(it)?.definitelyInitialized() ?: false)
            }

            fun handleLeakingThis(expression: KtExpression) {
                if (!finalClass) {
                    trace.record(
                        BindingContext.LEAKING_THIS, target(expression),
                        LeakingThisDescriptor.NonFinalClass(classDescriptor, classOrObject)
                    )
                } else {
                    val uninitializedProperty = firstUninitializedNotNullProperty()
                    if (uninitializedProperty != null) {
                        trace.record(
                            BindingContext.LEAKING_THIS, target(expression),
                            LeakingThisDescriptor.PropertyIsNull(uninitializedProperty, classOrObject)
                        )
                    }
                }
            }

            if (instruction.owner != pseudocode) {
                return@traverse
            }

            if (instruction is KtElementInstruction) {
                val element = instruction.element
                when (instruction) {
                    is ReadValueInstruction ->
                        if (element is KtThisExpression) {
                            if (!safeThisUsage(element)) {
                                handleLeakingThis(element)
                            }
                        }
                    is MagicInstruction ->
                        if (instruction.kind == MagicKind.IMPLICIT_RECEIVER) {
                            if (element is KtCallExpression) {
                                if (!safeCallUsage(element)) {
                                    handleLeakingThis(element)
                                }
                            } else if (element is KtReferenceExpression) {
                                if (!safeReferenceUsage(element)) {
                                    handleLeakingThis(element)
                                }
                            }
                        }
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun check(
            constructor: KtSecondaryConstructor,
            trace: BindingTrace,
            pseudocode: Pseudocode,
            pseudocodeVariablesData: PseudocodeVariablesData
        ) = check(constructor.getContainingClassOrObject(), trace, pseudocode, pseudocodeVariablesData)

        @JvmStatic
        fun check(
            classOrObject: KtClassOrObject,
            trace: BindingTrace,
            pseudocode: Pseudocode,
            pseudocodeVariablesData: PseudocodeVariablesData
        ) {
            val classDescriptor = trace.get(BindingContext.CLASS, classOrObject) ?: return
            ConstructorConsistencyChecker(classOrObject, classDescriptor, trace, pseudocode, pseudocodeVariablesData).check()
        }

        private fun target(expression: KtExpression): KtExpression = when (expression) {
            is KtThisExpression -> {
                val selectorOrThis = (expression.parent as? KtQualifiedExpression)?.let {
                    if (it.receiverExpression === expression) it.selectorExpression else null
                } ?: expression
                if (selectorOrThis === expression) selectorOrThis else target(selectorOrThis)
            }
            is KtCallExpression -> expression.let { it.calleeExpression ?: it }
            else -> expression
        }
    }
}
