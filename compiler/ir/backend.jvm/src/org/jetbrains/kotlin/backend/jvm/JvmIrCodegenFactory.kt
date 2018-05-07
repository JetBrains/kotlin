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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.PackageContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.ClassBodyGenerationMode
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

class JvmIrCodegenFactory(private val bodyGenerationMode: ClassBodyGenerationMode = ClassBodyGenerationMode.FULL_CLASS) : CodegenFactory {
    var irModuleFragment: IrModuleFragment? = null
    var generatorContext: GeneratorContext? = null

    private fun getOrBuildIr(state: GenerationState, files: Collection<KtFile>): IrModuleFragment {
        if (irModuleFragment == null) {
            doBuildIr(state, files)
        }
        return irModuleFragment!!
    }

    private fun getOrBuildContext(state: GenerationState, files: Collection<KtFile>): GeneratorContext {
        if (generatorContext == null) {
            doBuildIr(state, files)
        }
        return generatorContext!!
    }

    private fun doBuildIr(state: GenerationState, files: Collection<KtFile>) {
        val configuration = Psi2IrConfiguration()
        val psi2ir = Psi2IrTranslator(configuration)
        val psi2irContext = GeneratorContext(configuration, state.module, state.bindingContext, bodyGenerationMode)
        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files)

        generatorContext = psi2irContext
        this.irModuleFragment = irModuleFragment
    }

    override fun generateModule(state: GenerationState, files: Collection<KtFile?>, errorHandler: CompilationErrorHandler) {
        assert(!files.any { it == null })
        files as Collection<KtFile>
        JvmBackendFacade.doGenerateFilesInternal(state, errorHandler, getOrBuildIr(state, files), getOrBuildContext(state, files))
    }

    override fun createPackageCodegen(
        state: GenerationState,
        files: Collection<KtFile>,
        fqName: FqName,
        registry: PackagePartRegistry
    ): PackageCodegen {
        val impl = PackageCodegenImpl(state, files, fqName, registry)
        val context = getOrBuildContext(state, files)
        val ir = getOrBuildIr(state, files)

        val jvmBackendContext = JvmBackendContext(state, context.sourceManager, context.irBuiltIns, ir, context.symbolTable)
        val jvmBackend = JvmBackend(
            jvmBackendContext,
            if (bodyGenerationMode == ClassBodyGenerationMode.LIGHT_CLASS)
                LightClassesLower(jvmBackendContext)
            else
                JvmLower(jvmBackendContext)
        )

        return object : PackageCodegen {
            override fun generate(errorHandler: CompilationErrorHandler) {
                ir.files.forEach { jvmBackend.generateFile(it) }
            }

            override fun generateClassOrObject(classOrObject: KtClassOrObject, packagePartContext: PackageContext) {
                val correspondingIrFile = ir.files.first {
                    it.fileEntry.name == classOrObject.containingKtFile.virtualFilePath
                }

                val correspondingIrClass =
                    correspondingIrFile.declarations.find { it.descriptor.name.asString() == classOrObject.name } as IrClass

                jvmBackend.generateClass(correspondingIrClass)
            }

            override fun getPackageFragment(): PackageFragmentDescriptor {
                return impl.packageFragment
            }
        }
    }

    override fun createMultifileClassCodegen(
        state: GenerationState,
        files: Collection<KtFile>,
        fqName: FqName,
        registry: PackagePartRegistry
    ): MultifileClassCodegen {
        val context = getOrBuildContext(state, files)
        val ir = getOrBuildIr(state, files)

        val jvmBackendContext = JvmBackendContext(state, context.sourceManager, context.irBuiltIns, ir, context.symbolTable)
        val jvmBackend = JvmBackend(
            jvmBackendContext,
            if (bodyGenerationMode == ClassBodyGenerationMode.LIGHT_CLASS)
                LightClassesLower(jvmBackendContext)
            else
                JvmLower(jvmBackendContext)
        )

        return object : MultifileClassCodegen {
            override fun generate(errorHandler: CompilationErrorHandler) {
                ExternalDependenciesGenerator(context.symbolTable, context.irBuiltIns).generateUnboundSymbolsAsDependencies(ir)
                for (irFile in ir.files) {
                    try {
                        jvmBackend.generateFile(irFile)
                        state.afterIndependentPart()
                    } catch (e: Throwable) {
                        errorHandler.reportException(e, null) // TODO ktFile.virtualFile.url
                    }
                }
            }

            override fun generateClassOrObject(
                classOrObject: KtClassOrObject,
                packagePartContext: FieldOwnerContext<PackageFragmentDescriptor>
            ) {
                val correspondingIrFile = ir.files.first {
                    it.fileEntry.name == classOrObject.containingKtFile.virtualFilePath
                }
                jvmBackend.generateFile(correspondingIrFile)
            }
        }
    }
}