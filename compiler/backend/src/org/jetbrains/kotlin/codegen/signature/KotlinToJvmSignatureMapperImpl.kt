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

package org.jetbrains.kotlin.codegen.signature

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.KotlinToJvmSignatureMapper

class KotlinToJvmSignatureMapperImpl : KotlinToJvmSignatureMapper {
    // We use empty BindingContext, because it is only used by KotlinTypeMapper for purposes irrelevant to the needs of this class
    private val typeMapper = KotlinTypeMapper(BindingContext.EMPTY, ClassBuilderMode.LIGHT_CLASSES, NoResolveFileClassesProvider, null,
                                              IncompatibleClassTracker.DoNothing, JvmAbi.DEFAULT_MODULE_NAME, null)

    override fun mapToJvmMethodSignature(function: FunctionDescriptor) = typeMapper.mapAsmMethod(function)
}
