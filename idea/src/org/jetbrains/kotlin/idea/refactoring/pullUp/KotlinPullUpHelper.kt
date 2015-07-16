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
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.memberPullUp.PullUpData
import com.intellij.refactoring.memberPullUp.PullUpHelper
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiverOrThis
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import java.util.ArrayList
import java.util.LinkedHashSet

class KotlinPullUpHelper(
        private val javaData: PullUpData,
        private val data: KotlinPullUpData
) : PullUpHelper<MemberInfoBase<PsiMember>> {
    companion object {
        private var JetElement.newFqName: FqName? by CopyableUserDataProperty(Key.create("NEW_FQ_NAME"))
        private var JetElement.replaceWithTargetThis: Boolean? by CopyableUserDataProperty(Key.create("REPLACE_WITH_TARGET_THIS"))
        private var JetElement.newTypeText: String? by CopyableUserDataProperty(Key.create("NEW_TYPE_TEXT"))
    }

    private fun willBeUsedInSourceClass(member: PsiElement): Boolean {
        return !ReferencesSearch
                .search(member, LocalSearchScope(data.sourceClass), false)
                .all { it.getElement().parentsWithSelf.any { it in data.membersToMove } }
    }

    private fun liftToProtected(declaration: JetNamedDeclaration, ignoreUsages: Boolean = false) {
        if (!declaration.hasModifier(JetTokens.PRIVATE_KEYWORD)) return
        if (ignoreUsages || willBeUsedInSourceClass(declaration)) declaration.addModifierWithSpace(JetTokens.PROTECTED_KEYWORD)
    }

    override fun setCorrectVisibility(info: MemberInfoBase<PsiMember>) {
        val member = info.getMember().namedUnwrappedElement as? JetNamedDeclaration ?: return

        if (data.targetClass.isInterface()) {
            member.removeModifier(JetTokens.PUBLIC_KEYWORD)
            return
        }

        if (member.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
            member.accept(
                    object: JetVisitorVoid() {
                        override fun visitNamedDeclaration(declaration: JetNamedDeclaration) {
                            when (declaration) {
                                is JetClass -> {
                                    liftToProtected(declaration)
                                    declaration.getDeclarations().forEach { it.accept(this) }
                                }
                                is JetNamedFunction, is JetProperty -> {
                                    liftToProtected(declaration, declaration == member && info.isToAbstract())
                                }
                            }
                        }
                    }
            )
        }
    }

    override fun encodeContextInfo(info: MemberInfoBase<PsiMember>) {

    }

    private fun getClashingMemberInTargetClass(memberDescriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
        val memberInSuper = memberDescriptor.substitute(data.sourceToTargetClassSubstitutor) ?: return null
        return data.targetClassDescriptor.findCallableMemberBySignature(memberInSuper as CallableMemberDescriptor)
    }

    private fun fixOverrideAndGetClashingSuper(sourceMember: JetCallableDeclaration,
                                               targetMember: JetCallableDeclaration): JetCallableDeclaration? {
        val memberDescriptor = data.memberDescriptors[sourceMember] as CallableMemberDescriptor

        if (memberDescriptor.getOverriddenDescriptors().isEmpty()) {
            targetMember.removeOverrideModifier()
            return null
        }

        val clashingSuperDescriptor = getClashingMemberInTargetClass(memberDescriptor) ?: return null
        if (clashingSuperDescriptor.getOverriddenDescriptors().isEmpty()) {
            targetMember.removeOverrideModifier()
        }
        return clashingSuperDescriptor.getSource().getPsi() as? JetCallableDeclaration
    }

    private fun makeAbstract(sourceMember: JetCallableDeclaration, targetMember: JetCallableDeclaration) {
        if (!data.targetClass.isInterface()) {
            targetMember.addModifierWithSpace(JetTokens.ABSTRACT_KEYWORD)
        }

        if (sourceMember.getTypeReference() == null) {
            var type = (data.memberDescriptors[sourceMember] as CallableMemberDescriptor).getReturnType()
            if (type == null || type.isError()) {
                type = KotlinBuiltIns.getInstance().getNullableAnyType()
            }
            else {
                type = data.sourceToTargetClassSubstitutor.substitute(type, Variance.INVARIANT)
                       ?: KotlinBuiltIns.getInstance().getNullableAnyType()
            }

            if (sourceMember is JetProperty || !type.isUnit()) {
                val typeRef = JetPsiFactory(sourceMember).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
                targetMember.setTypeReference(typeRef)
            }
        }

        val deleteFrom = when (sourceMember) {
            is JetProperty -> {
                targetMember as JetProperty
                val accessors = targetMember.getAccessors()
                targetMember.getEqualsToken() ?: targetMember.getDelegate() ?: accessors.firstOrNull()
            }

            is JetNamedFunction -> {
                targetMember as JetNamedFunction
                targetMember.getEqualsToken() ?: targetMember.getBodyExpression()
            }

            else -> null
        }

        if (deleteFrom != null) {
            targetMember.deleteChildRange(deleteFrom, targetMember.getLastChild())
        }
    }

    private fun addMemberToTarget(targetMember: JetNamedDeclaration): JetNamedDeclaration {
        val anchor = data.targetClass.getDeclarations().filterIsInstance(targetMember.javaClass).lastOrNull()
        val movedMember = when {
            anchor == null && targetMember is JetProperty -> data.targetClass.addDeclarationBefore(targetMember, null)
            else -> data.targetClass.addDeclarationAfter(targetMember, anchor)
        }
        return movedMember as JetNamedDeclaration
    }

    private fun doAddCallableMember(memberCopy: JetCallableDeclaration, clashingSuper: JetCallableDeclaration?): JetCallableDeclaration {
        if (clashingSuper != null && clashingSuper.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            return clashingSuper.replaced(memberCopy)
        }
        return addMemberToTarget(memberCopy) as JetCallableDeclaration
    }

    private fun markElements(member: JetNamedDeclaration): List<JetElement> {
        val affectedElements = ArrayList<JetElement>()

        member.accept(
                object: JetVisitorVoid() {
                    private fun visitSuperOrThis(expression: JetInstanceExpressionWithLabel) {
                        val referenceTarget = data.sourceClassContext[BindingContext.REFERENCE_TARGET, expression.getInstanceReference()]
                        if (referenceTarget == data.targetClassDescriptor) {
                            expression.replaceWithTargetThis = true
                            affectedElements.add(expression)
                        }
                    }

                    override fun visitElement(element: PsiElement) {
                        element.allChildren.forEach { it.accept(this) }
                    }

                    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                        val resolvedCall = expression.getResolvedCall(data.sourceClassContext) ?: return
                        var receiver = resolvedCall.getExplicitReceiverValue()
                        if (!receiver.exists()) {
                            receiver = resolvedCall.getExtensionReceiver()
                        }
                        if (!receiver.exists()) {
                            receiver = resolvedCall.getDispatchReceiver()
                        }
                        if (!receiver.exists()) return

                        val implicitThis = receiver.getType().getConstructor().getDeclarationDescriptor() as? ClassDescriptor ?: return
                        if (implicitThis.isCompanionObject()
                            && DescriptorUtils.isAncestor(data.sourceClassDescriptor, implicitThis, true)) {
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
                        val oldType = data.sourceClassContext[BindingContext.TYPE, typeReference] ?: return
                        val newType = data.sourceToTargetClassSubstitutor.substitute(oldType, Variance.INVARIANT) ?: return
                        typeReference.newTypeText = IdeDescriptorRenderers.SOURCE_CODE.renderType(newType)
                        affectedElements.add(typeReference)
                    }
                }
        )

        return affectedElements
    }

    private fun processMarkedElements(member: JetNamedDeclaration) {
        val psiFactory = JetPsiFactory(member)
        val targetThis = psiFactory.createExpression("this@${data.targetClassDescriptor.getName().asString()}")
        val shorteningOptionsForThis = ShortenReferences.Options(removeThisLabels = true, removeThis = true)

        member.accept(
                object: JetVisitorVoid() {
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
                        visitSuperOrThis(expression)
                    }

                    override fun visitSuperExpression(expression: JetSuperExpression) {
                        visitSuperOrThis(expression)
                    }

                    override fun visitTypeReference(typeReference: JetTypeReference) {
                        typeReference.newTypeText?.let {
                            typeReference.newTypeText = null

                            (typeReference.replace(psiFactory.createType(it)) as JetElement).addToShorteningWaitSet()
                        }
                    }
                }
        )
    }

    private fun clearMarking(markedElements: List<JetElement>) {
        markedElements.forEach {
            it.newFqName = null
            it.newTypeText = null
            it.replaceWithTargetThis = null
        }
    }

    // TODO: Formatting rules don't apply here for some reason
    private fun JetNamedDeclaration.addModifierWithSpace(modifier: JetModifierKeywordToken) {
        addModifier(modifier)
        addAfter(JetPsiFactory(this).createWhiteSpace(), getModifierList())
    }

    override fun move(info: MemberInfoBase<PsiMember>, substitutor: PsiSubstitutor) {
        val member = info.getMember().namedUnwrappedElement as? JetNamedDeclaration ?: return
        val markedElements = markElements(member)
        val memberCopy = member.copy() as JetNamedDeclaration

        try {
            var movedMember: JetNamedDeclaration
            when (member) {
                is JetCallableDeclaration -> {
                    val clashingSuper = fixOverrideAndGetClashingSuper(member, memberCopy as JetCallableDeclaration)

                    val originalIsAbstract = member.hasModifier(JetTokens.ABSTRACT_KEYWORD)
                    val toAbstract = when {
                        info.isToAbstract() -> true
                        !data.targetClass.isInterface() -> false
                        member is JetProperty -> member.mustBeAbstractInInterface()
                        else -> false
                    }
                    if (toAbstract) {
                        if (!originalIsAbstract) {
                            makeAbstract(member, memberCopy)
                        }

                        movedMember = doAddCallableMember(memberCopy, clashingSuper)
                        if (member.getTypeReference() == null) {
                            movedMember.getTypeReference()?.addToShorteningWaitSet()
                        }
                        if (originalIsAbstract) {
                            member.delete()
                        }
                        else if (!member.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
                            member.addModifierWithSpace(JetTokens.OVERRIDE_KEYWORD)
                        }
                    }
                    else {
                        movedMember = doAddCallableMember(memberCopy, clashingSuper)
                        member.delete()
                    }

                    if (originalIsAbstract && data.targetClass.isInterface()) {
                        movedMember.removeModifier(JetTokens.ABSTRACT_KEYWORD)
                    }

                    if (!data.targetClass.isInterface()
                        && !data.targetClass.hasModifier(JetTokens.ABSTRACT_KEYWORD)
                        && (movedMember.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
                        data.targetClass.addModifierWithSpace(JetTokens.ABSTRACT_KEYWORD)
                    }

                    movedMember.getRepresentativeLightMethod()?.let { javaData.getMovedMembers().add(it) }
                }

                is JetClassOrObject -> {
                    if (data.targetClass.isInterface()) {
                        memberCopy.removeModifier(JetTokens.INNER_KEYWORD)
                    }
                    movedMember = addMemberToTarget(memberCopy) as JetClassOrObject
                    member.delete()
                    movedMember.toLightClass()?.let { javaData.getMovedMembers().add(it) }
                }

                else -> return
            }

            processMarkedElements(movedMember)
        }
        finally {
            clearMarking(markedElements)
        }
    }

    override fun postProcessMember(member: PsiMember) {

    }

    override fun moveFieldInitializations(movedFields: LinkedHashSet<PsiField>) {

    }

    override fun updateUsage(element: PsiElement) {

    }
}
