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

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

interface IncompatibleClassTracker {
    fun record(binaryClass: KotlinJvmBinaryClass)

    object DoNothing : IncompatibleClassTracker {
        override fun record(binaryClass: KotlinJvmBinaryClass) {
        }
    }
}

class IncompatibleClassTrackerImpl(val trace: BindingTrace) : IncompatibleClassTracker {
    private val classes = linkedSetOf<String>()

    override fun record(binaryClass: KotlinJvmBinaryClass) {
        if (classes.add(binaryClass.location)) {
            val errorData = IncompatibleVersionErrorData(
                    binaryClass.classHeader.bytecodeVersion,
                    JvmBytecodeBinaryVersion.INSTANCE,
                    binaryClass.location,
                    binaryClass.classId
            )
            trace.record(BYTECODE_VERSION_ERRORS, binaryClass.location, errorData)
        }
    }

    companion object {
        @JvmField
        val BYTECODE_VERSION_ERRORS: WritableSlice<String, IncompatibleVersionErrorData<JvmBytecodeBinaryVersion>> = Slices.createCollectiveSlice()
    }
}
