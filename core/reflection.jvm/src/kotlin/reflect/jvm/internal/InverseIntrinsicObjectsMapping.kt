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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.IntrinsicObjects
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName

object InverseIntrinsicObjectsMapping {
    private val map = linkedMapOf<FqName, ClassDescriptor>()

    init {
        for (descriptor in KotlinBuiltIns.getInstance().getBuiltInsPackageFragment().getMemberScope().getAllDescriptors()) {
            val companion = (descriptor as? ClassDescriptor)?.getCompanionObjectDescriptor() ?: continue
            IntrinsicObjects.mapType(companion)?.let { fqName ->
                map[fqName] = companion
            }
        }
    }

    /**
     * Maps a FQ name of an internal JVM class representing a built-in companion object to the relevant Kotlin descriptor,
     * e.g. [kotlin.jvm.internal.StringCompanionObject] -> class descriptor of [kotlin.String.Companion]
     */
    fun mapJvmClassToKotlinDescriptor(javaFqName: FqName): ClassDescriptor? = map[javaFqName]
}
