/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameterList
import com.intellij.psi.impl.light.LightFieldBuilder
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.impl.light.LightPsiClassBase
import com.intellij.psi.impl.light.LightTypeParameterBuilder
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.computeModifiersByPsi
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.allConstructors

internal class KtFakeLightClassForKlib(
    private val ktOrigin: KtClassOrObject,
    psiManager: PsiManager,
    name: String,
    private val _containingFile: KtFile,
    private val _containingClass: PsiClass? = null,
) : LightPsiClassBase(psiManager, ktOrigin.language, name) {

    override fun getQualifiedName(): String? {
        return ktOrigin.fqName?.asString()
    }

    private val _modifierList: PsiModifierList by lazyPub {
        // TODO: `final` modality, compute abstract members
        val modifiers = ktOrigin.computeModifiersByPsi { false }
        LightModifierList(manager, language, *modifiers.toTypedArray())
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isValid(): Boolean = ktOrigin.isValid

    override fun isWritable(): Boolean = ktOrigin.isWritable

    override fun getNavigationElement(): PsiElement = ktOrigin

    private val _extendsList by lazyPub {
        createInheritanceList(forExtendsList = true)
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList

    private val _implementsList by lazyPub {
        createInheritanceList(forExtendsList = false)
    }

    override fun getImplementsList(): PsiReferenceList? = _implementsList

    private fun createInheritanceList(forExtendsList: Boolean): PsiReferenceList {
        val role = if (forExtendsList) PsiReferenceList.Role.EXTENDS_LIST else PsiReferenceList.Role.IMPLEMENTS_LIST
        val listBuilder = KotlinSuperTypeListBuilder(
            this,
            ktOrigin.getSuperTypeList(),
            manager,
            language,
            role,
        )
        // TODO: need to map type reference to PsiType and add it to the list
        return listBuilder
    }

    override fun getChildren(): Array<out PsiElement?> {
        return arrayOf(
            *fields,
            *methods,
            *innerClasses,
        )
    }

    private val _fields: Array<out PsiField> by lazyPub {
        ktOrigin.declarations.filterIsInstance<KtProperty>().map { ktProperty ->
            LightFieldBuilder(
                manager,
                ktProperty.name.orAnonymous(ktProperty),
                PsiType.VOID, // TODO: property return type
            ).apply {
                containingClass = this@KtFakeLightClassForKlib
            }
        }.toTypedArray()
    }

    override fun getFields(): Array<out PsiField?> = _fields

    private val _methods: Array<out PsiMethod> by lazyPub {
        (ktOrigin.allConstructors + ktOrigin.declarations.filterIsInstance<KtNamedFunction>()).map { ktFunction ->
            LightMethodBuilder(
                manager,
                language,
                ktFunction.name.orAnonymous(ktFunction)
            ).apply {
                containingClass = this@KtFakeLightClassForKlib
                isConstructor = ktFunction is KtConstructor<*>
                // TODO: `null` return type for now
            }
        }.toTypedArray()
    }

    override fun getMethods(): Array<out PsiMethod?> = _methods

    private val _innerClasses: Array<out PsiClass> by lazyPub {
        ktOrigin.declarations.filterIsInstance<KtClassOrObject>().map { innerClassOrObject ->
            KtFakeLightClassForKlib(
                innerClassOrObject,
                manager,
                innerClassOrObject.name.orAnonymous(innerClassOrObject),
                _containingFile,
                this@KtFakeLightClassForKlib,
            )
        }.toTypedArray()
    }

    override fun getInnerClasses(): Array<out PsiClass?> = _innerClasses

    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun getScope(): PsiElement? = _containingFile

    override fun getContainingFile(): PsiFile? = _containingFile

    override fun getContainingClass(): PsiClass? = _containingClass

    private val _typeParameterList: PsiTypeParameterList by lazyPub {
        val listBuilder = LightTypeParameterListBuilder(manager, language)
        ktOrigin.typeParameters.forEachIndexed { i, ktTypeParameter ->
            val tp = LightTypeParameterBuilder(ktTypeParameter.name ?: "T$i", this, i)
            // TODO: need to populate references for upper bounds
            listBuilder.addParameter(tp)
        }
        listBuilder
    }

    override fun getTypeParameterList(): PsiTypeParameterList = _typeParameterList
}
