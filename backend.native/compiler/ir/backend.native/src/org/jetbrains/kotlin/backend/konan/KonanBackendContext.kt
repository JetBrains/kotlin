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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.KonanSharedVariablesManager
import org.jetbrains.kotlin.backend.konan.descriptors.konanInternal
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.Name

abstract internal class KonanBackendContext(val config: KonanConfig) : CommonBackendContext {
    abstract override val builtIns: KonanBuiltIns

    abstract override val ir: KonanIr

    override val sharedVariablesManager by lazy {
        // Creating lazily because builtIns module seems to be incomplete during `link` test;
        // TODO: investigate this.
        KonanSharedVariablesManager(this)
    }

    override fun getInternalClass(name: String): ClassDescriptor =
            builtIns.konanInternal.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    override fun getInternalFunctions(name: String): List<FunctionDescriptor> =
            builtIns.konanInternal.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).toList()

    val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        val location = element?.getCompilerMessageLocation(irFile ?: error("irFile should be not null for $element"))
        this.messageCollector.report(
                if (isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
                message, location
        )
    }

    private fun IrElement.getCompilerMessageLocation(containingFile: IrFile): CompilerMessageLocation? {
        val sourceRangeInfo = containingFile.fileEntry.getSourceRangeInfo(this.startOffset, this.endOffset)
        return CompilerMessageLocation.create(
                path = sourceRangeInfo.filePath,
                line = sourceRangeInfo.startLineNumber + 1,
                column = sourceRangeInfo.startColumnNumber + 1,
                lineContent = null // TODO: retrieve the line content.
        )
    }

}
