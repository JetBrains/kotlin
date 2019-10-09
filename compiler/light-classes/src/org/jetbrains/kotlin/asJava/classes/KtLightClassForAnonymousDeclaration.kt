/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.reference.SoftReference
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal open class KtLightClassForAnonymousDeclaration(classOrObject: KtClassOrObject) :
    KtLightClassForLocalDeclaration(classOrObject), PsiAnonymousClass {

    private var cachedBaseType: SoftReference<PsiClassType>? = null

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement {
        return JavaPsiFacade.getElementFactory(classOrObject.project).createReferenceElementByType(baseClassType)
    }

    @Synchronized
    override fun getBaseClassType(): PsiClassType {
        var type: PsiClassType? = null
        if (cachedBaseType != null) type = cachedBaseType!!.get()
        if (type != null && type.isValid) return type

        val firstSupertypeFQName = getFirstSupertypeFQNameForAnonymousDeclaration()
        for (superType in superTypes) {
            val superClass = superType.resolve()
            if (superClass != null && firstSupertypeFQName == superClass.qualifiedName) {
                type = superType
                break
            }
        }

        if (type == null) {
            val project = classOrObject.project
            type = PsiType.getJavaLangObject(PsiManager.getInstance(project), GlobalSearchScope.allScope(project))
        }

        cachedBaseType = SoftReference<PsiClassType>(type)
        return type
    }

    override fun getArgumentList(): PsiExpressionList? = null

    override fun isInQualifiedNew(): Boolean {
        return false
    }

    override fun getName(): String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.java != other::class.java) return false

        val aClass = other as KtLightClassForAnonymousDeclaration

        return classOrObject == aClass.classOrObject
    }

    override fun hashCode(): Int {
        return classOrObject.hashCode()
    }

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (baseClass is KtLightClassForSourceDeclaration) {
            return super.isInheritor(baseClass, checkDeep)
        }

        return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
    }

    override fun getNameIdentifier(): KtLightIdentifier? = null
    override fun getModifierList(): PsiModifierList? = null
    override fun hasModifierProperty(name: String): Boolean = name == PsiModifier.FINAL
    override fun getExtendsList(): PsiReferenceList? = null
    override fun getImplementsList(): PsiReferenceList? = null
    override fun getContainingClass(): PsiClass? = null
    override fun isInterface() = false
    override fun isAnnotationType() = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun isEnum() = false

    override fun copy(): PsiElement = KtLightClassForAnonymousDeclaration(classOrObject)

    companion object {
        fun KtLightClassForSourceDeclaration.getFirstSupertypeFQNameForAnonymousDeclaration(): String {
            val descriptor = getDescriptor() ?: return CommonClassNames.JAVA_LANG_OBJECT

            val superTypes = descriptor.typeConstructor.supertypes

            if (superTypes.isEmpty()) return CommonClassNames.JAVA_LANG_OBJECT

            val superType = superTypes.iterator().next()
            val superClassDescriptor = superType.constructor.declarationDescriptor

            if (superClassDescriptor === null) {
                // return java.lang.Object for recovery
                return CommonClassNames.JAVA_LANG_OBJECT
            }

            return DescriptorUtils.getFqName(superClassDescriptor).asString()
        }
    }
}
