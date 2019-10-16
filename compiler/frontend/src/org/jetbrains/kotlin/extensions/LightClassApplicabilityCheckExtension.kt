/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration

enum class LightClassApplicabilityType {
    LightClass,
    UltraLightClass
}

/**
 * This extension point needs to be implemented by the Kotlin compiler plugins in case they change the requested declaration (i.e. producing synthetic parts)
 * with backend extension points. The light class provider will request this EP to check either to create LightClass or UltraLight class implementations.
 * @see org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
 * @see org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
 */
interface LightClassApplicabilityCheckExtension {
    companion object : ProjectExtensionDescriptor<LightClassApplicabilityCheckExtension>(
        "org.jetbrains.kotlin.lightClassApplicabilityCheckExtension",
        LightClassApplicabilityCheckExtension::class.java
    )

    /**
     * This method should return LightClass if any changes in Kotlin declarations is going to be produced during JVM-backend code generation.
     * This method ought to be as fast as possible but never return UltraLightClass when not sure.
     * Next advice could be given to make best:
     * 1) Try to cover as much cases as possible without forcing descriptor evaluation
     * 2) After you've forced descriptor evaluation, ideally, you should never return LightClass while in fact no synthetic parts going to be generated
     * 3) So, you should force descriptor evaluation only if you're sure that you will be able to return UltraLightClass significantly more often
     * @see org.jetbrains.kotlin.noarg.ide.IdeNoArgApplicabilityExtension
     * @see org.jetbrains.kotlin.android.parcel.IDEParcelableApplicabilityExtension
     */
    fun checkApplicabilityType(declaration: KtDeclaration, descriptor: Lazy<DeclarationDescriptor?>): LightClassApplicabilityType
}