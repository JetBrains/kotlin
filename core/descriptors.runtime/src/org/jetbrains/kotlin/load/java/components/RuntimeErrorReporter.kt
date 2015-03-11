/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass

public object RuntimeErrorReporter : ErrorReporter {
    // TODO: specialized exceptions
    override fun reportIncompleteHierarchy(descriptor: ClassDescriptor, unresolvedSuperClasses: MutableList<String>) {
        throw IllegalStateException("Incomplete hierarchy for class ${descriptor.getName()}, unresolved classes $unresolvedSuperClasses")
    }

    override fun reportIncompatibleAbiVersion(kotlinClass: KotlinJvmBinaryClass, actualVersion: Int) {
        throw IllegalStateException("Incompatible ABI version of ${kotlinClass.getClassId()}: $actualVersion " +
                                    "(expected version is ${JvmAbi.VERSION})")
    }

    override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
        // TODO: use DescriptorRenderer
        throw IllegalStateException("Cannot infer visibility for class ${descriptor.getName()}")
    }

    override fun reportLoadingError(message: String, exception: Exception?) {
        throw IllegalStateException(message, exception)
    }
}
