/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.ElementPresentationUtil
import com.intellij.ui.IconManager
import com.intellij.util.IncorrectOperationException
import com.intellij.util.PlatformIcons
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import javax.swing.Icon

internal abstract class FirLightField protected constructor(
    private val containingClass: KtLightClass,
    lightMemberOrigin: LightMemberOrigin?,
) : FirLightMemberImpl<PsiField>(lightMemberOrigin, containingClass), KtLightField {

    override val clsDelegate: PsiField get() = invalidAccess()

    override fun setInitializer(initializer: PsiExpression?) = cannotModify()

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        basicIsEquivalentTo(this, another as? PsiField)

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun getParent() = containingClass
    override fun getContainingClass() = containingClass
    override fun getContainingFile(): PsiFile? = containingClass.containingFile
    override fun hasInitializer(): Boolean = initializer !== null

    override fun computeConstantValue(): Any? = null //TODO _constantInitializer?.value

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? = computeConstantValue()

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    override fun toString(): String = "KtLightField:$name"

    override fun getTypeElement(): PsiTypeElement? = null

    @Throws(IncorrectOperationException::class)
    override fun normalizeDeclaration() {
    }

    override fun isVisibilitySupported(): Boolean = true

    override fun getElementIcon(flags: Int): Icon? {
        val baseIcon = IconManager.getInstance().createLayeredIcon(
            this,
            PlatformIcons.VARIABLE_ICON, ElementPresentationUtil.getFlags(
                this,
                false
            )
        )
        return ElementPresentationUtil.addVisibilityIcon(this, flags, baseIcon)
    }

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitField(this)
        } else {
            visitor.visitElement(this)
        }
    }

    internal class FieldNameGenerator {
        private val usedNames: MutableSet<String> = mutableSetOf()

        fun generateUniqueFieldName(base: String): String {
            if (usedNames.add(base)) return base
            var i = 1
            while (true) {
                val suggestion = "$base$$i"
                if (usedNames.add(suggestion)) return suggestion
                i++
            }
        }
    }
}