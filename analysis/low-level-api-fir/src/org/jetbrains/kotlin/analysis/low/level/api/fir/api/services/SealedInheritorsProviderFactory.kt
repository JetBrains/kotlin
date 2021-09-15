/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.services

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSealedClassInheritorsProcessor
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

abstract class FirSealedClassInheritorsProcessorFactory {
    /**
     * Creates [SealedClassInheritorsProvider] for a module.
     * This function should crate a new [SealedClassInheritorsProvider] instance on every call.
     */
    abstract fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider
}

/**
 * Creates [SealedClassInheritorsProvider] for a module.
 * This function should crate a new [SealedClassInheritorsProvider] instance on every call.
 */
internal fun Project.createSealedInheritorsProvider(): SealedClassInheritorsProvider =
    getService(FirSealedClassInheritorsProcessorFactory::class.java).createSealedClassInheritorsProvider()