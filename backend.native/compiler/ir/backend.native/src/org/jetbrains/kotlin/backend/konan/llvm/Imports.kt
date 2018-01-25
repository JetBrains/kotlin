/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal interface LlvmImports {
    fun add(origin: LlvmSymbolOrigin)
}

internal val DeclarationDescriptor.llvmSymbolOrigin: LlvmSymbolOrigin
    get() {
        assert(!this.isExpectMember) { this }

        val module = this.module
        val moduleOrigin = module.origin
        when (moduleOrigin) {
            is LlvmSymbolOrigin -> return moduleOrigin
            SyntheticModules -> error("Declaration is synthetic and can't be an origin of LLVM symbol:\n${this}")
        }
    }

internal val Context.standardLlvmSymbolsOrigin: LlvmSymbolOrigin get() = this.stdlibModule.llvmSymbolOrigin
