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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import java.util.*

private var JetElement.newFqName: FqName? by CopyableUserDataProperty(Key.create("NEW_FQ_NAME"))
private var JetElement.replaceWithTargetThis: Boolean? by CopyableUserDataProperty(Key.create("REPLACE_WITH_TARGET_THIS"))
private var JetElement.newTypeText: ((TypeSubstitutor) -> String?)? by CopyableUserDataProperty(Key.create("NEW_TYPE_TEXT"))

fun markElements(
        declaration: JetNamedDeclaration,
        context: BindingContext,
        sourceClassDescriptor: ClassDescriptor, targetClassDescriptor: ClassDescriptor?
): List<JetElement> {
    val affectedElements = ArrayList<JetElement>()

    declaration.accept(
            object : JetVisitorVoid() {
                private fun visitSuperOrThis(expression: JetInstanceExpressionWithLabel) {
                    if (targetClassDescriptor == null) return

                    val referenceTarget = context[BindingContext.REFERENCE_TARGET, expression.instanceReference]
                    if (referenceTarget == targetClassDescriptor) {
                        expression.replaceWithTargetThis = true
                        affectedElements.add(expression)
                    }
                }

                override fun visitElement(element: PsiElement) {
                    element.allChildren.forEach { it.accept(this) }
                }

                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val resolvedCall = expression.getResolvedCall(context) ?: return
                    var receiver = resolvedCall.getExplicitReceiverValue()
                    if (!receiver.exists()) {
                        receiver = resolvedCall.extensionReceiver
                    }
                    if (!receiver.exists()) {
                        receiver = resolvedCall.dispatchReceiver
                    }
                    if (!receiver.exists()) return

                    val implicitThis = receiver.type.constructor.declarationDescriptor as? ClassDescriptor ?: return
                    if (implicitThis.isCompanionObject
                        && DescriptorUtils.isAncestor(sourceClassDescriptor, implicitThis, true)) {
                        val qualifierFqName = implicitThis.importableFqName ?: return

                        expression.newFqName = FqName("${qualifierFqName.asString()}.${expression.getReferencedName()}")
                        affectedElements.add(expression)
                    }
                }

                override fun visitThisExpression(expression: JetThisExpression) {
                    visitSuperOrThis(expression)
                }

                override fun visitSuperExpression(expression: JetSuperExpression) {
                    visitSuperOrThis(expression)
                }

                override fun visitTypeReference(typeReference: JetTypeReference) {
                    val oldType = context[BindingContext.TYPE, typeReference] ?: return
                    typeReference.newTypeText = f@ { substitutor ->
                        substitutor.substitute(oldType, Variance.INVARIANT)?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) }
                    }
                    affectedElements.add(typeReference)
                }
            }
    )

    return affectedElements
}

fun applyMarking(
        declaration: JetNamedDeclaration,
        substitutor: TypeSubstitutor, targetClassDescriptor: ClassDescriptor
) {
    val psiFactory = JetPsiFactory(declaration)
    val targetThis = psiFactory.createExpression("this@${targetClassDescriptor.name.asString()}")
    val shorteningOptionsForThis = ShortenReferences.Options(removeThisLabels = true, removeThis = true)

    declaration.accept(
            object : JetVisitorVoid() {
                private fun visitSuperOrThis(expression: JetInstanceExpressionWithLabel) {
                    expression.replaceWithTargetThis?.let {
                        expression.replaceWithTargetThis = null

                        val newThisExpression = expression.replace(targetThis) as JetExpression
                        newThisExpression.getQualifiedExpressionForReceiverOrThis().addToShorteningWaitSet(shorteningOptionsForThis)
                    }
                }

                override fun visitElement(element: PsiElement) {
                    for (it in element.allChildren.toList()) {
                        it.accept(this)
                    }
                }

                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    expression.newFqName?.let {
                        expression.newFqName = null

                        expression.mainReference.bindToFqName(it)
                    }
                }

                override fun visitThisExpression(expression: JetThisExpression) {
                    this.visitSuperOrThis(expression)
                }

                override fun visitSuperExpression(expression: JetSuperExpression) {
                    this.visitSuperOrThis(expression)
                }

                override fun visitTypeReference(typeReference: JetTypeReference) {
                    typeReference.newTypeText?.let f@ {
                        typeReference.newTypeText = null

                        val newTypeText = it(substitutor) ?: return@f
                        (typeReference.replace(psiFactory.createType(newTypeText)) as JetElement).addToShorteningWaitSet()
                    }
                }
            }
    )
}

fun clearMarking(markedElements: List<JetElement>) {
    markedElements.forEach {
        it.newFqName = null
        it.newTypeText = null
        it.replaceWithTargetThis = null
    }
}