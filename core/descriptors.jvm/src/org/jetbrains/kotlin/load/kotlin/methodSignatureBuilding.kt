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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import java.util.*

inline fun <T> signatures(block: SignatureBuildingComponents.() -> T) = with(SignatureBuildingComponents, block)

object SignatureBuildingComponents {
    fun javaLang(name: String) = "java/lang/$name"
    fun javaUtil(name: String) = "java/util/$name"
    fun javaFunction(name: String) = "java/util/function/$name"

    fun constructors(vararg signatures: String) = signatures.map { "<init>($it)V" }.toTypedArray()

    fun inJavaLang(name: String, vararg signatures: String) = inClass(javaLang(name), *signatures)
    fun inJavaUtil(name: String, vararg signatures: String) = inClass(javaUtil(name), *signatures)

    fun inClass(internalName: String, vararg signatures: String) = signatures.mapTo(LinkedHashSet()) { internalName + "." + it }

    fun signature(classDescriptor: ClassDescriptor, jvmDescriptor: String) = signature(classDescriptor.internalName, jvmDescriptor)
    fun signature(classId: ClassId, jvmDescriptor: String) = signature(classId.internalName, jvmDescriptor)
    fun signature(internalName: String, jvmDescriptor: String) = internalName + "." + jvmDescriptor

    fun jvmDescriptor(name: String, vararg parameters: String, ret: String = "V") =
            jvmDescriptor(name, parameters.asList(), ret)
    fun jvmDescriptor(name: String, parameters: List<String>, ret: String = "V") =
            "$name(${parameters.joinToString("") { escapeClassName(it) }})${escapeClassName(internalName = ret)}"

    private fun escapeClassName(internalName: String) = if (internalName.length > 1) "L$internalName;" else internalName
}
