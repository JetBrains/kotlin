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

import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.moveInner.MoveInnerClassUsagesHandler
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.isToBeShortened
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf

sealed class MoveDeclarationsDelegate {
    abstract fun getContainerChangeInfo(originalDeclaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): ContainerChangeInfo

    open fun findInternalUsages(descriptor: MoveDeclarationsDescriptor): List<UsageInfo> = emptyList()

    open fun collectConflicts(
            descriptor: MoveDeclarationsDescriptor,
            internalUsages: MutableSet<UsageInfo>,
            conflicts: MultiMap<PsiElement, String>
    ) {

    }

    open fun preprocessDeclaration(descriptor: MoveDeclarationsDescriptor, originalDeclaration: KtNamedDeclaration) {

    }

    open fun preprocessUsages(descriptor: MoveDeclarationsDescriptor, usages: List<UsageInfo>) {

    }

    object TopLevel : MoveDeclarationsDelegate() {
        override fun getContainerChangeInfo(originalDeclaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): ContainerChangeInfo {
            val sourcePackage = ContainerInfo.Package(originalDeclaration.containingKtFile.packageFqName)
            val targetPackage = moveTarget.targetContainerFqName?.let { ContainerInfo.Package(it) } ?: ContainerInfo.UnknownPackage
            return ContainerChangeInfo(sourcePackage, targetPackage)
        }
    }

    class NestedClass(
            val newClassName: String? = null,
            val outerInstanceParameterName: String? = null
    ) : MoveDeclarationsDelegate() {
        override fun getContainerChangeInfo(originalDeclaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): ContainerChangeInfo {
            val originalInfo = ContainerInfo.Class(originalDeclaration.containingClassOrObject!!.fqName!!)
            val movingToClass = (moveTarget as? KotlinMoveTargetForExistingElement)?.targetElement is KtClassOrObject
            val targetContainerFqName = moveTarget.targetContainerFqName
            val newInfo = when {
                targetContainerFqName == null -> ContainerInfo.UnknownPackage
                movingToClass -> ContainerInfo.Class(targetContainerFqName)
                else -> ContainerInfo.Package(targetContainerFqName)
            }
            return ContainerChangeInfo(originalInfo, newInfo)
        }

        override fun findInternalUsages(descriptor: MoveDeclarationsDescriptor): List<UsageInfo> {
            val classToMove = descriptor.elementsToMove.singleOrNull() as? KtClass ?: return emptyList()
            return collectOuterInstanceReferences(classToMove)
        }

        private fun isValidTargetForImplicitCompanionAsDispatchReceiver(
                moveDescriptor: MoveDeclarationsDescriptor,
                companionDescriptor: ClassDescriptor
        ): Boolean {
            val moveTarget = moveDescriptor.moveTarget
            return when (moveTarget) {
                is KotlinMoveTargetForCompanion -> true
                is KotlinMoveTargetForExistingElement -> {
                    val targetClass = moveTarget.targetElement as? KtClassOrObject ?: return false
                    val targetClassDescriptor = targetClass.unsafeResolveToDescriptor() as ClassDescriptor
                    val companionClassDescriptor = companionDescriptor.containingDeclaration as? ClassDescriptor ?: return false
                    targetClassDescriptor.isSubclassOf(companionClassDescriptor)
                }
                else -> false
            }
        }

        override fun collectConflicts(
                descriptor: MoveDeclarationsDescriptor,
                internalUsages: MutableSet<UsageInfo>,
                conflicts: MultiMap<PsiElement, String>
        ) {
            val usageIterator = internalUsages.iterator()
            while (usageIterator.hasNext()) {
                val usage = usageIterator.next()
                val element = usage.element ?: continue

                val isConflict = when (usage) {
                    is ImplicitCompanionAsDispatchReceiverUsageInfo -> {
                        if (!isValidTargetForImplicitCompanionAsDispatchReceiver(descriptor, usage.companionDescriptor)) {
                            conflicts.putValue(element, "Implicit companion object will be inaccessible: ${element.text}")
                        }
                        true
                    }

                    is OuterInstanceReferenceUsageInfo -> usage.reportConflictIfAny(conflicts)

                    else -> false
                }
                if (isConflict) {
                    usageIterator.remove()
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
                        val type = (containingClassOrObject!!.unsafeResolveToDescriptor() as ClassDescriptor).defaultType
                        val parameter = KtPsiFactory(project)
                                .createParameter("private val $outerInstanceParameterName: ${IdeDescriptorRenderers.SOURCE_CODE.renderType(type)}")
                        createPrimaryConstructorParameterListIfAbsent().addParameter(parameter).isToBeShortened = true
                    }
                }
            }
        }

        override fun preprocessUsages(descriptor: MoveDeclarationsDescriptor, usages: List<UsageInfo>) {
            if (outerInstanceParameterName == null) return
            val psiFactory = KtPsiFactory(descriptor.project)
            val newOuterInstanceRef = psiFactory.createExpression(outerInstanceParameterName)
            val classToMove = descriptor.elementsToMove.singleOrNull() as? KtClass

            for (usage in usages) {
                if (usage is MoveRenameUsageInfo) {
                    val referencedNestedClass = usage.referencedElement?.unwrapped as? KtClassOrObject
                    if (referencedNestedClass == classToMove) {
                        val outerClass = referencedNestedClass?.containingClassOrObject
                        val lightOuterClass = outerClass?.toLightClass()
                        if (lightOuterClass != null) {
                            MoveInnerClassUsagesHandler.EP_NAME
                                    .forLanguage(usage.element!!.language)
                                    ?.correctInnerClassUsage(usage, lightOuterClass)
                        }
                    }
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