/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.decompiler.textBuilder

import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass
import com.intellij.openapi.diagnostic.Logger

class LoggingErrorReporter(private val log: Logger) : ErrorReporter {
    override fun reportLoadingError(message: String, exception: Exception?) {
        log.error(message, exception)
    }

    override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
        log.error("Could not infer visibility for $descriptor")
    }

    override fun reportIncompatibleAbiVersion(kotlinClass: KotlinJvmBinaryClass, actualVersion: Int) {
        log.error("Incompatible ABI version for class ${kotlinClass.getClassId()}, actual version: $actualVersion")
    }
}
