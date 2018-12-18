/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k.postProcessing

import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.setDefaultValue
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessing
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.setModifierList
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

class ConvertDataClass : J2kPostProcessing {
    override val writeActionNeeded: Boolean = true

    fun KtCallableDeclaration.rename(newName: String) {
        val factory = KtPsiFactory(this)
        ReferencesSearch.search(this, LocalSearchScope(containingKtFile)).forEach {
            it.element.replace(factory.createExpression(newName))
        }
        setName(newName)
    }

    private data class DataClassInfo(
        val constructorParameter: KtParameter,
        val property: KtProperty,
        val initBlockStatement: KtBinaryExpression
    )

    private fun collectPropertiesData(klass: KtClass): List<DataClassInfo> =
        klass.getAnonymousInitializers()
            .flatMap { (it.body as KtBlockExpression).statements }
            .asSequence()
            .mapNotNull { it.asAssignment() }
            .mapNotNull { statement ->
                val property =
                    statement.left
                        ?.unpackedReferenceToProperty()
                        ?.takeIf { it.containingClass() == klass } ?: return@mapNotNull null
                if (property.getter != null || property.setter != null) return@mapNotNull null
                val constructorParameter =
                    ((statement.right as? KtReferenceExpression)
                        ?.references
                        ?.firstOrNull { it is KtSimpleNameReference }
                        ?.resolve() as? KtParameter)
                        ?.takeIf {
                            it.containingClass() == klass && !it.hasValOrVar()
                        } ?: return@mapNotNull null

                if (constructorParameter.type() != property.type()) return@mapNotNull null

                DataClassInfo(constructorParameter, property, statement)
            }.toList()

    override fun createAction(element: KtElement, diagnostics: Diagnostics): (() -> Unit)? {
        if (element !is KtClass) return null
        return {
            val factory = KtPsiFactory(element)
            for ((constructorParameter, property, statement) in collectPropertiesData(element)) {
                constructorParameter.addBefore(property.valOrVarKeyword, constructorParameter.nameIdentifier!!)
                constructorParameter.addAfter(factory.createWhiteSpace(), constructorParameter.valOrVarKeyword!!)
                constructorParameter.rename(property.name!!)
                if (property.modifierList != null) {
                    constructorParameter.setModifierList(property.modifierList!!)
                }
                property.delete()
                statement.delete()
            }
            for (initBlock in element.getAnonymousInitializers()) {
                if ((initBlock.body as KtBlockExpression).statements.isEmpty()) {
                    initBlock.delete()
                }
            }
        }
    }
}