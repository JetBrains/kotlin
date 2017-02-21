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

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.reference.SoftReference
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal open class KtLightClassForAnonymousDeclaration(classOrObject: KtClassOrObject) :
        KtLightClassForLocalDeclaration(classOrObject), PsiAnonymousClass {

    private var cachedBaseType: SoftReference<PsiClassType>? = null

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement {
        return JavaPsiFacade.getElementFactory(classOrObject.project).createReferenceElementByType(baseClassType)
    }

    private val firstSupertypeFQName: String
        get() {
            val descriptor = getDescriptor() ?: return CommonClassNames.JAVA_LANG_OBJECT

            val superTypes = descriptor.typeConstructor.supertypes

            if (superTypes.isEmpty()) return CommonClassNames.JAVA_LANG_OBJECT

            val superType = superTypes.iterator().next()
            val superClassDescriptor = superType.constructor.declarationDescriptor

            if (superClassDescriptor == null) {
                LOG.error("No declaration descriptor for supertype " + superType + " of " + getDescriptor())

                // return java.lang.Object for recovery
                return CommonClassNames.JAVA_LANG_OBJECT
            }

            return DescriptorUtils.getFqName(superClassDescriptor).asString()
        }

    @Synchronized override fun getBaseClassType(): PsiClassType {
        var type: PsiClassType? = null
        if (cachedBaseType != null) type = cachedBaseType!!.get()
        if (type != null && type.isValid) return type

        val firstSupertypeFQName = firstSupertypeFQName
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

    override fun getNameIdentifier() = null
    override fun getModifierList(): PsiModifierList? = null
    override fun hasModifierProperty(name: String): Boolean = name == PsiModifier.FINAL
    override fun getExtendsList() = null
    override fun getImplementsList() = null
    override fun getContainingClass(): PsiClass? = null
    override fun isInterface() = false
    override fun isAnnotationType() = false
    override fun getTypeParameterList() = null
    override fun isEnum() = false

    override fun copy(): PsiElement = KtLightClassForAnonymousDeclaration(classOrObject)

    companion object {
        private val LOG = Logger.getInstance(KtLightClassForAnonymousDeclaration::class.java)
    }
}
