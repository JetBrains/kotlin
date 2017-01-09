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

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.descriptors.SpecialDescriptorsFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

class JvmBackendContext(
        val state: GenerationState,
        val psiSourceManager: PsiSourceManager,
        override val irBuiltIns: IrBuiltIns
) : BackendContext {
    override val builtIns = state.module.builtIns
    val specialDescriptorsFactory = SpecialDescriptorsFactory(psiSourceManager, builtIns)
    override val sharedVariablesManager = JvmSharedVariablesManager(builtIns)
}
