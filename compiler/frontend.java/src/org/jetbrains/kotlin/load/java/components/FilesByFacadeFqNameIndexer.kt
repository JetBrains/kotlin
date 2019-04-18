/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.FilePreprocessorExtension
import org.jetbrains.kotlin.resolve.addElementToSlice
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

// TODO: this component is actually only needed by CLI, see CliLightClassGenerationSupport
class FilesByFacadeFqNameIndexer(private val trace: BindingTrace) : FilePreprocessorExtension {
    override fun preprocessFile(file: KtFile) {
        if (!file.hasTopLevelCallables()) return

        trace.addElementToSlice(FACADE_FILES_BY_FQ_NAME, file.javaFileFacadeFqName, file)
        trace.addElementToSlice(FACADE_FILES_BY_PACKAGE_NAME, file.javaFileFacadeFqName.parent(), file)
    }

    companion object {
        @JvmField
        val FACADE_FILES_BY_FQ_NAME: WritableSlice<FqName, MutableCollection<KtFile>> = Slices.createSimpleSlice()

        @JvmField
        val FACADE_FILES_BY_PACKAGE_NAME: WritableSlice<FqName, MutableCollection<KtFile>> = Slices.createSimpleSlice()

        init {
            BasicWritableSlice.initSliceDebugNames(FilesByFacadeFqNameIndexer::class.java)
        }
    }
}
