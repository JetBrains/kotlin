/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

inline fun File.writeToFileUsingSmartPrinterIfFileContentChanged(block: SmartPrinter.() -> Unit) {
    val newText = buildString { SmartPrinter(this).block() }
    GeneratorsFileUtil.writeFileIfContentChanged(this, newText, logNotChanged = false)
}
