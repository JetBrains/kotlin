/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment

object KotlinLightClassFactory {
    fun createClass(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? =
        CachedValuesManager.getCachedValue(classOrObject) {
            CachedValueProvider.Result.create(
                createClassNoCache(classOrObject),
                KotlinModificationTrackerService.getInstance(classOrObject.project).outOfBlockModificationTracker,
            )
        }

    fun createClassNoCache(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? {
        val containingFile = classOrObject.containingFile
        if (containingFile is KtCodeFragment) {
            // Avoid building light classes for code fragments
            return null
        }

        if (classOrObject.shouldNotBeVisibleAsLightClass()) {
            return null
        }

        return LightClassGenerationSupport.getInstance(classOrObject.project).run {
            createUltraLightClass(classOrObject)
        }
    }
}