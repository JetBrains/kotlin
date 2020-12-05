/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

fun findMainClass(bindingContext: BindingContext, languageVersionSettings: LanguageVersionSettings, files: List<KtFile>): FqName? {
    val mainFunctionDetector = MainFunctionDetector(bindingContext, languageVersionSettings)
    return files.asSequence()
        .map { file ->
            if (mainFunctionDetector.hasMain(file.declarations))
                JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName
            else
                null
        }
        .singleOrNull { it != null }
}
