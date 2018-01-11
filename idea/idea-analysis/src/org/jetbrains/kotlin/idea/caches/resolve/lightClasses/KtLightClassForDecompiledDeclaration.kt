/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject

class KtLightClassForDecompiledDeclaration(
        override val clsDelegate: ClsClassImpl,
        override val kotlinOrigin: KtClassOrObject?,
        private val file: KtClsFile
) : KtLightClassBase(clsDelegate.manager) {
    val fqName = kotlinOrigin?.fqName ?: FqName(clsDelegate.qualifiedName)

    override fun copy() = this

    override fun getOwnInnerClasses(): List<PsiClass> {
        val nestedClasses = kotlinOrigin?.declarations?.filterIsInstance<KtClassOrObject>() ?: emptyList()
        return clsDelegate.ownInnerClasses.map { innerClsClass ->
            KtLightClassForDecompiledDeclaration(innerClsClass as ClsClassImpl,
                                                 nestedClasses.firstOrNull { innerClsClass.name == it.name }, file)
        }
    }

    override fun getOwnFields(): List<PsiField> {
        return clsDelegate.ownFields.map { KtLightFieldImpl.create(LightMemberOriginForCompiledField(it, file), it, this) }
    }

    override fun getOwnMethods(): List<PsiMethod> {
        return clsDelegate.ownMethods.map { KtLightMethodImpl.create(it, LightMemberOriginForCompiledMethod(it, file), this) }
    }

    override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: file

    override fun getParent() = clsDelegate.parent

    override fun equals(other: Any?): Boolean =
            other is KtLightClassForDecompiledDeclaration &&
            fqName == other.fqName

    override fun hashCode(): Int =
            fqName.hashCode()

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.BINARY
}

