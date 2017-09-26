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
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.impl.light.AbstractLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.LightClassInheritanceHelper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorUtils

// Used as a placeholder when actual light class does not exist (expect-classes, for example)
// The main purpose is to allow search of inheritors within hierarchies containing such classes
class KtFakeLightClass(override val kotlinOrigin: KtClassOrObject) :
        AbstractLightClass(kotlinOrigin.manager, KotlinLanguage.INSTANCE),
        KtLightClass {
    private val _delegate by lazy { PsiElementFactory.SERVICE.getInstance(kotlinOrigin.project).createClass(kotlinOrigin.name ?: "") }
    private val _containingClass by lazy { kotlinOrigin.containingClassOrObject?.let { KtFakeLightClass(it) } }

    override val clsDelegate get() = _delegate
    override val originKind get() = LightClassOriginKind.SOURCE

    override fun getDelegate() = _delegate
    override fun copy() = KtFakeLightClass(kotlinOrigin)

    override fun getQualifiedName() = kotlinOrigin.fqName?.asString()
    override fun getContainingClass() = _containingClass
    override fun getNavigationElement() = kotlinOrigin
    override fun getIcon(flags: Int) = kotlinOrigin.getIcon(flags)
    override fun getContainingFile() = kotlinOrigin.containingFile
    override fun getUseScope() = kotlinOrigin.useScope

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val baseKtClass = (baseClass as? KtLightClass)?.kotlinOrigin ?: return false
        val baseDescriptor = baseKtClass.resolveToDescriptorIfAny() as? ClassDescriptor ?: return false
        val thisDescriptor = kotlinOrigin.resolveToDescriptorIfAny() as? ClassDescriptor ?: return false
        return if (checkDeep) DescriptorUtils.isSubclass(thisDescriptor, baseDescriptor) else DescriptorUtils.isDirectSubclass(thisDescriptor, baseDescriptor)
    }
}