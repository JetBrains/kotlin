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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

fun PsiNamedElement.getAccessorNames(readable: Boolean = true, writable: Boolean = true): List<String> {
    fun PropertyAccessorsPsiMethods.toNameList(): List<String> {
        val getter = getter
        val setter = setter

        val result = ArrayList<String>()
        if (readable && getter != null) result.add(getter.getName())
        if (writable && setter != null) result.add(setter.getName())
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

public fun PsiNamedElement.getClassNameForCompanionObject(): String? {
    return if (this is KtObjectDeclaration && this.isCompanion()) {
        getNonStrictParentOfType<KtClass>()?.getName()
    } else {
        null
    }
}

public fun PsiNamedElement.getSpecialNamesToSearch(): List<String> {
    val name = getName()
    return when {
        name == null || !Name.isValidIdentifier(name) -> Collections.emptyList<String>()
        this is KtParameter -> {
            val componentFunctionName = this.dataClassComponentFunction()?.name
            if (componentFunctionName == null) return Collections.emptyList<String>()

            return listOf(componentFunctionName.asString(), KtTokens.LPAR.getValue())
        }
        else -> Name.identifier(name).getOperationSymbolsToSearch().map { (it as KtSingleValueToken).getValue() }
    }
}

fun KtParameter.dataClassComponentFunction(): FunctionDescriptor? {
    if (!hasValOrVar()) return null

    // Forcing full resolve of owner class: otherwise DATA_CLASS_COMPONENT_FUNCTION won't be calculated.
    // This is a hack, but better solution would need refactoring of data class component function resolution: they need to be resolved
    // when resolving corresponding property.
    LightClassUtil.getLightClassPropertyMethods(this)

    val context = this.analyze()
    val paramDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? ValueParameterDescriptor
    return context[BindingContext.DATA_CLASS_COMPONENT_FUNCTION, paramDescriptor]
}


