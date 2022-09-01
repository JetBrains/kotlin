/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtScript

class KotlinK1LightClassFactory : KotlinLightClassFactory {
    override fun createScript(script: KtScript): KtLightClassForScript? = CachedValuesManager.getCachedValue(script) {
        CachedValueProvider.Result.create(
            createScriptNoCache(script),
            KotlinModificationTrackerService.getInstance(script.project).outOfBlockModificationTracker,
        )
    }

    companion object {
        fun createScriptNoCache(script: KtScript): KtLightClassForScript? {
            val containingFile = script.containingFile
            if (containingFile is KtCodeFragment) {
                // Avoid building light classes for code fragments
                return null
            }

            return LightClassGenerationSupport.getInstance(script.project).createUltraLightClassForScript(script)
        }
    }
}