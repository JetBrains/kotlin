// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.jvm.JvmInline

open class Expando {
    val expansion: Expansion = Expansion()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Expansion(val map: MutableMap<String, Any?> = mutableMapOf()) {
    override inline fun toString(): String = "OK"
}

data class Foo(val i: Int): Expando() {
    override fun toString(): String {
        return "$expansion"
    }
}

fun box(): String = Foo(0).toString()
