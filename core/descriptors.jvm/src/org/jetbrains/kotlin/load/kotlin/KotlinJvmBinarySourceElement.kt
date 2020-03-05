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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class KotlinJvmBinarySourceElement(
    val binaryClass: KotlinJvmBinaryClass,
    override val incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>? = null,
    override val isPreReleaseInvisible: Boolean = false,
    override val isInvisibleIrDependency: Boolean = false
) : DeserializedContainerSource {
    override val presentableString: String
        get() = "Class '${binaryClass.classId.asSingleFqName().asString()}'"

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE

    override fun toString() = "${this::class.java.simpleName}: $binaryClass"
}
