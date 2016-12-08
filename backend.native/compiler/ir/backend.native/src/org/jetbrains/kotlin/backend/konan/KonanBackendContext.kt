package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.konan.llvm.KonanPlatform

open internal class KonanBackendContext : BackendContext {
    override val builtIns = KonanPlatform.builtIns

    override val sharedVariablesManager by lazy {
        TODO()
    }
}