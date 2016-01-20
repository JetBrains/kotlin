/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.moveInner.MoveInnerClassUsagesHandler
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

sealed class MoveDeclarationsDelegate {
    abstract fun getContainerChangeInfo(originalDeclaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): ContainerChangeInfo
    abstract fun findUsages(descriptor: MoveDeclarationsDescriptor): List<UsageInfo>
    abstract fun collectConflicts(usages: MutableList<UsageInfo>, conflicts: MultiMap<PsiElement, String>)
    abstract fun preprocessDeclaration(descriptor: MoveDeclarationsDescriptor, originalDeclaration: KtNamedDeclaration)
    abstract fun preprocessUsages(project: Project, usages: List<UsageInfo>)

    object TopLevel : MoveDeclarationsDelegate() {
        override fun getContainerChangeInfo(originalDeclaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): ContainerChangeInfo {
            return ContainerChangeInfo(ContainerInfo.Package(originalDeclaration.getContainingKtFile().packageFqName),
                                       ContainerInfo.Package(moveTarget.targetContainerFqName!!))
        }

        override fun findUsages(descriptor: MoveDeclarationsDescriptor): List<UsageInfo> = emptyList()

        override fun collectConflicts(usages: MutableList<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {

        }

        override fun preprocessDeclaration(descriptor: MoveDeclarationsDescriptor, originalDeclaration: KtNamedDeclaration) {

        }

        override fun preprocessUsages(project: Project, usages: List<UsageInfo>) {

        }
    }

    class NestedClass(
            val newClassName: String? = null,
            val outerInstanceParameterName: String? = null
    ) : MoveDeclarationsDelegate() {
        override fun getContainerChangeInfo(originalDeclaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): ContainerChangeInfo {
            val originalInfo = ContainerInfo.Class(originalDeclaration.containingClassOrObject!!.fqName!!)
            val movingToClass = (moveTarget as? KotlinMoveTargetForExistingElement)?.targetElement is KtClassOrObject
            val newInfo = if (movingToClass) {
                ContainerInfo.Class(moveTarget.targetContainerFqName!!)
            } else {
                ContainerInfo.Package(moveTarget.targetContainerFqName!!)
            }
            return ContainerChangeInfo(originalInfo, newInfo)
        }

        override fun findUsages(descriptor: MoveDeclarationsDescriptor): List<UsageInfo> {
            val classToMove = descriptor.elementsToMove.singleOrNull() as? KtClass ?: return emptyList()
            return collectOuterInstanceReferences(classToMove)
        }

        override fun collectConflicts(usages: MutableList<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
            val usageIterator = usages.iterator()
            while (usageIterator.hasNext()) {
                val usage = usageIterator.next();
                val element = usage.element ?: continue

                if (usage is ImplicitCompanionAsDispatchReceiverUsageInfo) {
                    conflicts.putValue(element, "Implicit companion object will be inaccessible: ${element.text}")
                    usageIterator.remove()
                    continue
                }

                if (usage !is OuterInstanceReferenceUsageInfo) continue

                if (usage.isIndirectOuter) {
                    conflicts.putValue(element, "Indirect outer instances won't be extracted: ${element.text}")
                    usageIterator.remove()
                }

                if (usage !is OuterInstanceReferenceUsageInfo.ImplicitReceiver) continue

                val fullCall = usage.callElement?.let { it.getQualifiedExpressionForSelector() ?: it } ?: continue
                when {
                    fullCall is KtQualifiedExpression -> {
                        conflicts.putValue(
                                fullCall,
                                "Qualified call won't be processed: ${fullCall.text}"
                        )
                        usageIterator.remove()
                    }

                    usage.isDoubleReceiver -> {
                        conflicts.putValue(
                                fullCall,
                                "Call with two implicit receivers won't be processed: ${fullCall.text}"
                        )
                        usageIterator.remove()
                    }
                }
            }
        }

        override fun preprocessDeclaration(descriptor: MoveDeclarationsDescriptor, originalDeclaration: KtNamedDeclaration) {
            with(originalDeclaration) {
                newClassName?.let { setName(it) }

                if (this is KtClass) {
                    if ((descriptor.moveTarget as? KotlinMoveTargetForExistingElement)?.targetElement !is KtClassOrObject) {
                        if (hasModifier(KtTokens.INNER_KEYWORD)) removeModifier(KtTokens.INNER_KEYWORD)
                        if (hasModifier(KtTokens.PROTECTED_KEYWORD)) removeModifier(KtTokens.PROTECTED_KEYWORD)
                    }

                    if (outerInstanceParameterName != null) {
                        val type = (containingClassOrObject!!.resolveToDescriptor() as ClassDescriptor).defaultType
                        val parameter = KtPsiFactory(project)
                                .createParameter("private val $outerInstanceParameterName: ${IdeDescriptorRenderers.SOURCE_CODE.renderType(type)}")
                        createPrimaryConstructorParameterListIfAbsent().addParameter(parameter)
                    }
                }
            }
        }

        override fun preprocessUsages(project: Project, usages: List<UsageInfo>) {
            if (outerInstanceParameterName == null) return
            val psiFactory = KtPsiFactory(project)
            val newOuterInstanceRef = psiFactory.createExpression(outerInstanceParameterName)

            for (usage in usages) {
                val referencedNestedClass = (usage as? MoveRenameUsageInfo)?.referencedElement?.unwrapped as? KtClassOrObject
                val outerClass = referencedNestedClass?.containingClassOrObject
                val lightOuterClass = outerClass?.toLightClass()
                if (lightOuterClass != null) {
                    MoveInnerClassUsagesHandler.EP_NAME
                            .forLanguage(usage.element!!.language)
                            ?.correctInnerClassUsage(usage, lightOuterClass)
                }

                when (usage) {
                    is OuterInstanceReferenceUsageInfo.ExplicitThis -> {
                        usage.expression?.replace(newOuterInstanceRef)
                    }
                    is OuterInstanceReferenceUsageInfo.ImplicitReceiver -> {
                        usage.callElement?.let { it.replace(psiFactory.createExpressionByPattern("$0.$1", outerInstanceParameterName, it)) }
                    }
                }
            }
        }
    }
}