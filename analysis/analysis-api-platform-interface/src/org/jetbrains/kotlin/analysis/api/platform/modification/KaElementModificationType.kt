/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.psi.PsiElement

/**
 * [KaElementModificationType] describes which kind of modification was applied to a changed [PsiElement]. [KaSourceModificationService]
 * uses this information to perform change locality detection.
 */
public sealed interface KaElementModificationType {
    /**
     * The element has been added as a new element.
     */
    public object ElementAdded : KaElementModificationType

    /**
     * The element passed is the parent of a removed element, which is additionally provided as [removedElement]. The removed element itself
     * cannot be the modification "anchor" because it has already been removed and is not part of the [KtFile][org.jetbrains.kotlin.psi.KtFile]
     * anymore, but it might still be used to determine the modification's change type.
     */
    public class ElementRemoved(public val removedElement: PsiElement) : KaElementModificationType

    /**
     * Which kind of modification was applied to the element is unknown.
     */
    public object Unknown : KaElementModificationType
}
