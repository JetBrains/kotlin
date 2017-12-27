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

import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.backend.konan.descriptors.EmptyDescriptorVisitorVoid
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.PhaseManager
import org.jetbrains.kotlin.backend.konan.KonanPhase

internal class DeserializerDriver(val context: Context) {

    private val cache = mutableMapOf<FunctionDescriptor, IrDeclaration?>()

    internal fun deserializeInlineBody(descriptor: FunctionDescriptor): IrDeclaration? = cache.getOrPut(descriptor) {
        if (!descriptor.needsSerializedIr) return null
        if (!descriptor.isDeserializableCallable) return null

        var deserializedIr: IrDeclaration? = null
        PhaseManager(context).phase(KonanPhase.DESERIALIZER) {
            context.log{"### IR deserialization attempt:\t$descriptor"}
            try {
                deserializedIr = IrDeserializer(context, descriptor).decodeDeclaration()
                context.log{"${deserializedIr!!.descriptor}"}
                context.log{ ir2stringWhole(deserializedIr!!) }
                context.log{"IR deserialization SUCCESS:\t$descriptor"}
            } catch(e: Throwable) {
                context.log{"IR deserialization FAILURE:\t$descriptor"}
                if (context.phase!!.verbose) e.printStackTrace()
            }
        }
        deserializedIr
    }

    internal fun dumpAllInlineBodies() {
        if (! context.phase!!.verbose) return
        context.log{"Now deserializing all inlines for debugging purpose."}
        context.moduleDescriptor.accept(
            InlineBodiesPrinterVisitor(InlineBodyPrinter()), Unit)
    }


    inner class InlineBodiesPrinterVisitor(worker: EmptyDescriptorVisitorVoid): DeepVisitor<Unit>(worker)

    inner class InlineBodyPrinter: EmptyDescriptorVisitorVoid() {

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit): Boolean {
            if (descriptor.isDeserializableCallable) {
                this@DeserializerDriver.deserializeInlineBody(descriptor)
            }

            return true
        }
    }
}
