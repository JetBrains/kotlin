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

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.backend.konan.descriptors.EmptyDescriptorVisitorVoid
import org.jetbrains.kotlin.backend.konan.descriptors.DeepVisitor
import org.jetbrains.kotlin.backend.konan.descriptors.needsInlining
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.llvm.base64Encode
import org.jetbrains.kotlin.backend.konan.llvm.base64Decode
import org.jetbrains.kotlin.backend.konan.PhaseManager
import org.jetbrains.kotlin.backend.konan.KonanPhase
import org.jetbrains.kotlin.serialization.MutableTypeTable
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.backend.common.validateIrFunction

internal class DeserializerDriver(val context: Context) {

    internal fun deserializeInlineBody(descriptor: FunctionDescriptor): IrDeclaration? {
        if (!descriptor.needsInlining) return null

        if (descriptor !is DeserializedSimpleFunctionDescriptor) {
            return null
        }

        var deserializedIr: IrDeclaration? = null
        PhaseManager(context).phase(KonanPhase.DESERIALIZER) {
            context.log("### IR deserialization attempt:\t$descriptor")
            try {
                deserializedIr = IrDeserializer(context, descriptor).decodeDeclaration()
                context.log("${deserializedIr!!.descriptor}")
                context.log(ir2stringWhole(deserializedIr!!))
                context.log("IR deserialization SUCCESS:\t$descriptor")
            } catch(e: Throwable) {
                context.log("IR deserialization FAILURE:\t$descriptor")
                e.printStackTrace()
            }
        }
        return deserializedIr
    }

    internal fun dumpAllInlineBodies() {
        if (! context.phase!!.verbose) return
        context.log("Now deserializing all inlines for debugging purpose.")
        context.moduleDescriptor!!.accept(
            InlineBodiesPrinterVisitor(InlineBodyPrinter()), Unit)
    }


    inner class InlineBodiesPrinterVisitor(worker: EmptyDescriptorVisitorVoid): DeepVisitor<Unit>(worker) { }

    inner class InlineBodyPrinter: EmptyDescriptorVisitorVoid() {

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit): Boolean {
            if (descriptor is DeserializedSimpleFunctionDescriptor) {
                this@DeserializerDriver.deserializeInlineBody(descriptor)
            }

            return true
        }
    }
}
