// !LANGUAGE: +JvmFieldInInterface
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

interface Foo {
    companion object {
        @JvmField
        val value = "OK"
    }
}

fun box(): String {
    val field = Foo.Companion::value.javaField!!
    return field.get(null) as String
}
