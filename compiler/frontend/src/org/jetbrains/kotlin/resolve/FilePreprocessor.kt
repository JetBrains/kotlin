/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Sets
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext.PACKAGE_TO_FILES

interface FilePreprocessorExtension {
    fun preprocessFile(file: KtFile)
}

class FilePreprocessor(
    private val trace: BindingTrace,
    private val extensions: Iterable<FilePreprocessorExtension>
) {
    fun preprocessFile(file: KtFile) {
        registerFileByPackage(file)

        for (extension in extensions) {
            extension.preprocessFile(file)
        }
    }

    private fun registerFileByPackage(file: KtFile) {
        // Register files corresponding to this package
        // The trace currently does not support bi-di multimaps that would handle this task nicer
        val fqName = file.packageFqName
        val files = trace.get(PACKAGE_TO_FILES, fqName) ?: Sets.newIdentityHashSet()
        files.add(file)
        trace.record(PACKAGE_TO_FILES, fqName, files)
    }
}