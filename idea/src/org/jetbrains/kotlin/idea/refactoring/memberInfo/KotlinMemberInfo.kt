/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinMemberInfo @JvmOverloads constructor(
    member: KtNamedDeclaration,
    val isSuperClass: Boolean = false,
    val isCompanionMember: Boolean = false
) : MemberInfoBase<KtNamedDeclaration>(member) {
    companion object {
        private val RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
            modifiers = setOf(DescriptorRendererModifier.INNER)
        }
    }

    init {
        val memberDescriptor = member.resolveToDescriptorWrapperAware()
        isStatic = member.parent is KtFile

        if ((member is KtClass || member is KtPsiClassWrapper) && isSuperClass) {
            if (member.isInterfaceClass()) {
                displayName = RefactoringBundle.message("member.info.implements.0", member.name)
                overrides = false
            } else {
                displayName = RefactoringBundle.message("member.info.extends.0", member.name)
                overrides = true
            }
        } else {
            displayName = RENDERER.render(memberDescriptor)
            if (member.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                displayName = "abstract $displayName"
            }
            if (isCompanionMember) {
                displayName = "companion $displayName"
            }

            val overriddenDescriptors = (memberDescriptor as? CallableMemberDescriptor)?.overriddenDescriptors ?: emptySet()
            if (overriddenDescriptors.isNotEmpty()) {
                overrides = overriddenDescriptors.any { it.modality != Modality.ABSTRACT }
            }
        }
    }
}

fun lightElementForMemberInfo(declaration: KtNamedDeclaration?): PsiMember? {
    return when (declaration) {
        is KtNamedFunction -> declaration.getRepresentativeLightMethod()
        is KtProperty, is KtParameter -> declaration.toLightElements().let {
            it.firstIsInstanceOrNull<PsiMethod>() ?: it.firstIsInstanceOrNull<PsiField>()
        } as PsiMember?
        is KtClassOrObject -> declaration.toLightClass()
        is KtPsiClassWrapper -> declaration.psiClass
        else -> null
    }
}

fun MemberInfoBase<out KtNamedDeclaration>.toJavaMemberInfo(): MemberInfo? {
    val declaration = member
    val psiMember: PsiMember? = lightElementForMemberInfo(declaration)
    val info = MemberInfo(psiMember ?: return null, psiMember is PsiClass && overrides != null, null)
    info.isToAbstract = isToAbstract
    info.isChecked = isChecked
    return info
}

fun MemberInfo.toKotlinMemberInfo(): KotlinMemberInfo? {
    val declaration = member.unwrapped as? KtNamedDeclaration ?: return null
    return KotlinMemberInfo(declaration, declaration is KtClass && overrides != null).apply {
        this.isToAbstract = this@toKotlinMemberInfo.isToAbstract
    }
}
