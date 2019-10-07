/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration

interface UltraLightClassCodegenSupport {
    fun interceptMethodsBuilding(
        declaration: KtDeclaration,
        descriptor: Lazy<DeclarationDescriptor?>,
        containingDeclaration: KtUltraLightClass,
        methodsList: MutableList<KtLightMethod>
    ) = Unit
}
