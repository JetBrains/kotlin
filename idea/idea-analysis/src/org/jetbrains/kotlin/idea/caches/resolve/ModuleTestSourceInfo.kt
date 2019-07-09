/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("GetModuleInfoKt")
@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo as getNullableModuleInfoNew

@Deprecated(
    "Temporary interface to support binary compatibility in other plugins. " +
            "Works only for instanceof check. Will be removed in Kotlin plugin bundled to 2018.2.",
    ReplaceWith("org.jetbrains.kotlin.idea.caches.project.ModuleTestSourceInfo"),
    level = DeprecationLevel.ERROR
)
interface IdeaModuleInfo : ModuleInfo

@Deprecated(
    "Temporary interface to support binary compatibility in other plugins. " +
            "Works only for instanceof check. Will be removed in Kotlin plugin bundled to 2018.2.",
    ReplaceWith("org.jetbrains.kotlin.idea.caches.project.ModuleTestSourceInfo"),
    level = DeprecationLevel.ERROR
)
interface ModuleTestSourceInfo : IdeaModuleInfo

@Deprecated(
    "Temporary function to support binary compatibility after for other plugins after move." +
            "Will be removed in Kotlin plugin bundled to 2018.2.",
    ReplaceWith("org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo"),
    level = DeprecationLevel.ERROR
)
fun PsiElement.getNullableModuleInfo(): IdeaModuleInfo? = getNullableModuleInfoNew()
