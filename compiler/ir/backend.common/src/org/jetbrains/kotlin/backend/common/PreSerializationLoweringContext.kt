/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * This backend context is used in the first compilation stage. Namely, it is passed to lowerings
 * that are run before serializing IR into a KLIB.
 */
class PreSerializationLoweringContext(
    override val irBuiltIns: IrBuiltIns,
    override val configuration: CompilerConfiguration,
) : CommonBackendContext {

    override val builtIns: KotlinBuiltIns
        get() = shouldNotBeCalled()

    override val mapping: Mapping = Mapping()

    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)

    override val ir: Ir<PreSerializationLoweringContext>
        get() = TODO("Not yet implemented") // Should be implemented in scope of KT-71415

    override val innerClassesSupport: InnerClassesSupport
        get() = TODO("Not yet implemented") // Should be implemented in scope of KT-71415

    override val sharedVariablesManager: SharedVariablesManager
        get() = TODO("Not yet implemented") // Should be implemented in scope of KT-71415

    override val internalPackageFqn: FqName
        get() = TODO("Not yet implemented") // Should be implemented in scope of KT-71415

    override val irFactory: IrFactory
        get() = IrFactoryImpl

    override var inVerbosePhase: Boolean = false
}
