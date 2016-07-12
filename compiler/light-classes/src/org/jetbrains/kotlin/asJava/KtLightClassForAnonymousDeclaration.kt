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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.reference.SoftReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal open class KtLightClassForAnonymousDeclaration(name: FqName,
                                                        classOrObject: KtClassOrObject) :
        KtLightClassForExplicitDeclaration(name, classOrObject), PsiAnonymousClass {

    private var cachedBaseType: SoftReference<PsiClassType>? = null

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement {
        return JavaPsiFacade.getElementFactory(classOrObject.project).createReferenceElementByType(baseClassType)
    }

    override fun getContainingClass(): PsiClass? {
        return delegate.containingClass
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

    companion object {
        private val LOG = Logger.getInstance(KtLightClassForAnonymousDeclaration::class.java)
    }
}
