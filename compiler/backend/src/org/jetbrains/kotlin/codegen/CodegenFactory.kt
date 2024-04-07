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

import com.intellij.openapi.project.Project
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.serialization.StringTableImpl

interface CodegenFactory {
    fun convertToIr(input: IrConversionInput): BackendInput

    // Extracts a part of the BackendInput which corresponds only to the specified source files.
    // This is needed to support cyclic module dependencies, which are allowed in JPS, where frontend and psi2ir is run on sources of all
    // modules combined, and then backend is run on each individual module.
    fun getModuleChunkBackendInput(wholeBackendInput: BackendInput, sourceFiles: Collection<KtFile>): BackendInput

    fun invokeLowerings(state: GenerationState, input: BackendInput): CodegenInput

    fun invokeCodegen(input: CodegenInput)

    fun generateModule(state: GenerationState, input: BackendInput) {
        val result = invokeLowerings(state, input)
        invokeCodegen(result)
    }

    class IrConversionInput(
        val project: Project,
        val files: Collection<KtFile>,
        val configuration: CompilerConfiguration,
        val module: ModuleDescriptor,
        val bindingContext: BindingContext,
        val languageVersionSettings: LanguageVersionSettings,
        val ignoreErrors: Boolean,
        val skipBodies: Boolean,
    ) {
        companion object {
            fun fromGenerationStateAndFiles(state: GenerationState, files: Collection<KtFile>): IrConversionInput =
                with(state) {
                    IrConversionInput(
                        project, files, configuration, module, originalFrontendBindingContext, languageVersionSettings, ignoreErrors,
                        skipBodies = !state.classBuilderMode.generateBodies
                    )
                }
        }
    }

    // These opaque interfaces are needed to transfer the result of psi2ir to lowerings to codegen.
    // Hopefully this can be refactored/simplified once the old JVM backend code is removed.
    interface BackendInput

    interface CodegenInput {
        val state: GenerationState
    }

    companion object {
        fun doCheckCancelled(state: GenerationState) {
            if (state.classBuilderMode.generateBodies) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            }
        }
    }
}

object DefaultCodegenFactory : CodegenFactory {
    private class OldBackendInput(val ktFiles: Collection<KtFile>) : CodegenFactory.BackendInput

    private class DummyOldCodegenInput(override val state: GenerationState) : CodegenFactory.CodegenInput

    override fun convertToIr(input: CodegenFactory.IrConversionInput): CodegenFactory.BackendInput = OldBackendInput(input.files)

    override fun getModuleChunkBackendInput(
        wholeBackendInput: CodegenFactory.BackendInput,
        sourceFiles: Collection<KtFile>,
    ): CodegenFactory.BackendInput = OldBackendInput(sourceFiles)

    override fun invokeLowerings(state: GenerationState, input: CodegenFactory.BackendInput): CodegenFactory.CodegenInput {
        input as OldBackendInput
        val filesInPackages = MultiMap<FqName, KtFile>()
        val filesInMultifileClasses = MultiMap<FqName, KtFile>()

        for (file in input.ktFiles) {
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

        return DummyOldCodegenInput(state)
    }

    override fun invokeCodegen(input: CodegenFactory.CodegenInput) {
        generateModuleMetadata(input)
    }

    private fun generateModuleMetadata(result: CodegenFactory.CodegenInput) {
        val builder = JvmModuleProtoBuf.Module.newBuilder()

        val stringTable = StringTableImpl()
        builder.addDataFromCompiledModule(stringTable, result.state)

        val (stringTableProto, qualifiedNameTableProto) = stringTable.buildProto()
        builder.setStringTable(stringTableProto)
        builder.setQualifiedNameTable(qualifiedNameTableProto)

        result.state.factory.setModuleMapping(builder.build())
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
