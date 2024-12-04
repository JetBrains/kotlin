/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult.UNSURE

interface LightClassInheritanceHelper {
    fun isInheritor(
        lightClass: KtLightClass,
        baseClass: PsiClass,
        checkDeep: Boolean
    ): ImpreciseResolveResult

    object NoHelp : LightClassInheritanceHelper {
        override fun isInheritor(lightClass: KtLightClass, baseClass: PsiClass, checkDeep: Boolean) = UNSURE
    }

    companion object {
        fun getService(project: Project): LightClassInheritanceHelper =
            project.getService(LightClassInheritanceHelper::class.java) ?: NoHelp
    }
}
