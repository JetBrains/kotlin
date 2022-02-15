/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact

// IR backend (JVM, JS, Native)
sealed class IrBackendInput : ResultingArtifact.BackendInput<IrBackendInput>() {
    override val kind: BackendKinds.IrBackend
        get() = BackendKinds.IrBackend

    abstract val irModuleFragment: IrModuleFragment

    data class JsIrBackendInput(
        override val irModuleFragment: IrModuleFragment,
        val sourceFiles: List<KtFile>,
        val bindingContext: BindingContext,
        val icData: List<KotlinFileSerializedData>,
        val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    ) : IrBackendInput()

    data class JvmIrBackendInput(
        val state: GenerationState,
        val codegenFactory: JvmIrCodegenFactory,
        val backendInput: JvmIrCodegenFactory.JvmIrBackendInput
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = backendInput.irModuleFragment
    }
}
