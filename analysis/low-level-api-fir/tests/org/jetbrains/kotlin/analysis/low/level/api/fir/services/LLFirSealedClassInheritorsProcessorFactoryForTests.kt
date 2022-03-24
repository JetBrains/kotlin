/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.services

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl

internal class LLFirSealedClassInheritorsProcessorFactoryForTests : FirSealedClassInheritorsProcessorFactory() {
    override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
        return SealedClassInheritorsProviderImpl
    }
}