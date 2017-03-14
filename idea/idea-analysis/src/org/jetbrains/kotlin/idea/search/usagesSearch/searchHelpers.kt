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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.LightClassUtil.PropertyAccessorsPsiMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import java.util.*

fun PsiNamedElement.getAccessorNames(readable: Boolean = true, writable: Boolean = true): List<String> {
    fun PropertyAccessorsPsiMethods.toNameList(): List<String> {
        val getter = getter
        val setter = setter

        val result = ArrayList<String>()
        if (readable && getter != null) result.add(getter.name)
        if (writable && setter != null) result.add(setter.name)
        return result
    }

    if (this !is KtDeclaration || KtPsiUtil.isLocal(this)) return Collections.emptyList()

    when (this) {
        is KtProperty ->
            return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
        is KtParameter ->
            if (hasValOrVar()) {
                return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
            }
    }

    return Collections.emptyList()
}

fun PsiNamedElement.getClassNameForCompanionObject(): String? {
    return if (this is KtObjectDeclaration && this.isCompanion()) {
        getNonStrictParentOfType<KtClass>()?.name
    } else {
        null
    }
}

fun KtParameter.dataClassComponentFunction(): FunctionDescriptor? {
    if (!isDataClassProperty()) return null

    val context = this.analyze()
    val paramDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? ValueParameterDescriptor

    val constructor = paramDescriptor?.containingDeclaration as? ConstructorDescriptor ?: return null
    val index = constructor.valueParameters.indexOf(paramDescriptor)
    val correspondingComponentName = DataClassDescriptorResolver.createComponentName(index + 1)

    val dataClass = constructor.containingDeclaration as? ClassDescriptor ?: return null
    dataClass.unsubstitutedMemberScope.getContributedFunctions(correspondingComponentName, NoLookupLocation.FROM_IDE)

    return context[BindingContext.DATA_CLASS_COMPONENT_FUNCTION, paramDescriptor]
}

fun KtParameter.isDataClassProperty(): Boolean {
    if (!hasValOrVar()) return false
    return this.containingClassOrObject?.hasModifier(KtTokens.DATA_KEYWORD) ?: false
}

