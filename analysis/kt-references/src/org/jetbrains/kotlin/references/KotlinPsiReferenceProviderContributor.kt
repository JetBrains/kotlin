/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references

import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.psi.KtElement

/**
 * Extension-point contributor for Kotlin PSI reference providers.
 *
 * The EP allows registering custom reference contributors for [org.jetbrains.kotlin.psi.KtElement]s.
 *
 * Contributors are supposed to be registered via the `org.jetbrains.kotlin.psiReferenceProvider` EP.
 * Each contributor binds a particular PSI element type (represented by [elementClass]) to a [referenceProvider] function
 * which may produce [PsiReference]s for a given element instance.
 *
 * Implementation notes for contributors:
 * - Keep [referenceProvider] fast and side‑effect free; return an empty list
 *   when no references should be produced.
 * - Perform inexpensive guards before any expensive analysis (e.g., quick kind
 *   checks, token/parent filters) to minimize overhead.
 */
interface KotlinPsiReferenceProviderContributor<T : KtElement> {
    /**
     * Functional interface used to create references for a specific PSI element
     * of type [T]. The function must be pure and must return a (possibly empty)
     * list of [PsiReference]s. Returning an empty list is preferred to doing
     * unnecessary work when the element is not applicable.
     */
    fun interface ReferenceProvider<in T : KtElement> : (T) -> List<PsiReference>

    /**
     * The base PSI class this contributor applies to. A contributor is
     * considered applicable to an element if `elementClass.isAssignableFrom(element.javaClass)`
     */
    val elementClass: Class<out T>

    /** The function that produces references for elements of [elementClass] */
    val referenceProvider: ReferenceProvider<T>
}
