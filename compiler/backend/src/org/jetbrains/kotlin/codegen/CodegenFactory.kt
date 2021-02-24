/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile

interface CodegenFactory {
    fun generateModule(state: GenerationState, files: Collection<KtFile>)

    companion object {
        fun doCheckCancelled(state: GenerationState) {
            if (state.classBuilderMode.generateBodies) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            }
        }
    }
}

object DefaultCodegenFactory : CodegenFactory {
    override fun generateModule(state: GenerationState, files: Collection<KtFile>) {
        val filesInPackages = MultiMap<FqName, KtFile>()
        val filesInMultifileClasses = MultiMap<FqName, KtFile>()

        for (file in files) {
            val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file)

            if (fileClassInfo.withJvmMultifileClass) {
                filesInMultifileClasses.putValue(fileClassInfo.facadeClassFqName, file)
            } else {
                filesInPackages.putValue(file.packageFqName, file)
            }
        }

        val obsoleteMultifileClasses = HashSet(state.obsoleteMultifileClasses)
        for (multifileClassFqName in filesInMultifileClasses.keySet() + obsoleteMultifileClasses) {
            CodegenFactory.doCheckCancelled(state)
            generateMultifileClass(state, multifileClassFqName, filesInMultifileClasses.get(multifileClassFqName))
        }

        val packagesWithObsoleteParts = HashSet(state.packagesWithObsoleteParts)
        for (packageFqName in packagesWithObsoleteParts + filesInPackages.keySet()) {
            CodegenFactory.doCheckCancelled(state)
            generatePackage(state, packageFqName, filesInPackages.get(packageFqName))
        }
    }

    private fun generateMultifileClass(state: GenerationState, multifileClassFqName: FqName, files: Collection<KtFile>) {
        state.factory.forMultifileClass(multifileClassFqName, files).generate()
    }

    fun generatePackage(
        state: GenerationState,
        packageFqName: FqName,
        ktFiles: Collection<KtFile>
    ) {
        // We do not really generate package class, but use old package fqName to identify package in module-info.
        //FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName);
        state.factory.forPackage(packageFqName, ktFiles).generate()
    }
}
