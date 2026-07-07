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

package org.jetbrains.kotlin.psi

/**
 * A service that maps a [KtDeclaration] between its "original" and its "navigation" element.
 *
 * These may differ when a declaration is viewed through more than one PSI representation — for example, a declaration
 * decompiled from a library exposes a synthetic element for display while the source (or another canonical element)
 * is the target of navigation. Implementations back [com.intellij.psi.PsiElement.getNavigationElement] and
 * [com.intellij.psi.PsiElement.getOriginalElement] for Kotlin declarations.
 */
interface KotlinDeclarationNavigationPolicy {
    /**
     * Returns the canonical/original element for the given declaration, or the declaration itself if there is no
     * distinct original.
     */
    fun getOriginalElement(declaration: KtDeclaration): KtElement

    /**
     * Returns the element that navigation (Go to Declaration and similar) should land on for the given declaration.
     */
    fun getNavigationElement(declaration: KtDeclaration): KtElement
}
