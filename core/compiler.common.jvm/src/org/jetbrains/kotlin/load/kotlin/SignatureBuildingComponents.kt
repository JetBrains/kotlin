/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*

inline fun <T> signatures(block: SignatureBuildingComponents.() -> T) = with(SignatureBuildingComponents, block)

object SignatureBuildingComponents {
    fun javaLang(name: String): String = "java/lang/$name"
    fun javaUtil(name: String): String = "java/util/$name"
    fun javaFunction(name: String): String = "java/util/function/$name"

    fun constructors(vararg signatures: String): Array<String> = signatures.map { "<init>($it)V" }.toTypedArray()

    fun inJavaLang(name: String, vararg signatures: String): Set<String> = inClass(javaLang(name), *signatures)
    fun inJavaUtil(name: String, vararg signatures: String): Set<String> = inClass(javaUtil(name), *signatures)

    fun inClass(internalName: String, vararg signatures: String): Set<String> = signatures.mapTo(LinkedHashSet()) { "$internalName.$it" }
    fun signature(classId: ClassId, jvmDescriptor: String): String = signature(classId.internalName, jvmDescriptor)
    fun signature(internalName: String, jvmDescriptor: String): String = "$internalName.$jvmDescriptor"

    fun jvmDescriptor(name: String, parameters: List<String>, ret: String = "V"): String =
        "$name(${parameters.joinToString("") { escapeClassName(it) }})${escapeClassName(internalName = ret)}"

    private fun escapeClassName(internalName: String): String = if (internalName.length > 1) "L$internalName;" else internalName
}

val ClassId.internalName: String
    get() {
        return JvmClassName.internalNameByClassId(JavaToKotlinClassMap.mapKotlinToJava(asSingleFqName().toUnsafe()) ?: this)
    }
