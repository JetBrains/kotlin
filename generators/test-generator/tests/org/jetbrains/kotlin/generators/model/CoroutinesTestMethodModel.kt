/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

class CoroutinesTestMethodModel(
    rootDir: File,
    file: File,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean,
    val isLanguageVersion1_3: Boolean
) : SimpleTestMethodModel(
    rootDir,
    file,
    filenamePattern,
    checkFilenameStartsLowerCase,
    targetBackend,
    skipIgnored
) {
    object Kind : MethodModel.Kind()

    override val kind: MethodModel.Kind
        get() = Kind

    override val name: String
        get() = super.name + if (isLanguageVersion1_3) "_1_3" else "_1_2"
}
