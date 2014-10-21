/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
public fun shouldChangeModificationCount(place: PsiElement): Boolean {
    // false -> inside code block
    // true -> means nothing, parent will be checked

    val declaration = PsiTreeUtil.getParentOfType<JetDeclaration>(place, javaClass<JetDeclaration>(), true)
    if (declaration == null) return true

    return when (declaration) {
        is JetNamedFunction -> {
            val function: JetNamedFunction = declaration
            if (function.hasDeclaredReturnType() || function.hasBlockBody()) {
                takePartInDeclarationTypeInference(function)
            }
            else {
                shouldChangeModificationCount(function)
            }
        }
        is JetPropertyAccessor -> {
            takePartInDeclarationTypeInference(declaration)
        }
        is JetProperty -> {
            val property = declaration as JetProperty
            if (property.getTypeReference() != null) {
                takePartInDeclarationTypeInference(property)
            }
            else {
                shouldChangeModificationCount(property)
            }
        }
        is JetMultiDeclaration, is JetMultiDeclarationEntry, is JetFunctionLiteral -> {
            shouldChangeModificationCount(declaration)
        }
        else -> {
            true
        }
    }
}

private fun takePartInDeclarationTypeInference(place: PsiElement): Boolean {
    val declaration = PsiTreeUtil.getParentOfType<JetDeclaration>(place, javaClass<JetDeclaration>(), true)
    if (declaration != null) {
        if (declaration is JetNamedFunction) {
            val function = declaration as JetNamedFunction
            if (!function.hasDeclaredReturnType() && !function.hasBlockBody()) {
                return true
            }
        }
        else if (declaration is JetProperty) {
            val property = declaration as JetProperty
            if (property.getTypeReference() == null) {
                return true
            }
        }

        return takePartInDeclarationTypeInference(declaration)
    }

    return false
}