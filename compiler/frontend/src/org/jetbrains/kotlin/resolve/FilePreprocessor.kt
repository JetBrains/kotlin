/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Sets
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext.PACKAGE_TO_FILES
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

@K1Deprecation
interface FilePreprocessorExtension {
    fun preprocessFile(file: KtFile)
}

@K1Deprecation
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
        trace.addElementToSlice(PACKAGE_TO_FILES, file.packageFqName, file)
    }
}

@K1Deprecation
fun <K, T> BindingTrace.addElementToSlice(
    slice: WritableSlice<K, MutableCollection<T>>, key: K, element: T
) {
    val elements = get(slice, key) ?: Sets.newIdentityHashSet()
    elements.add(element)
    record(slice, key, elements)
}
