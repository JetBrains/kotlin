// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// FILE: 1.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class C(val x: String)

// FILE: 2.kt
@file:JvmMultifileClass
@file:JvmName("Multifile")

private var result: String? = null

var String.k: C
    get() = C(this + result!!)
    set(value) { result = value.x }

// FILE: 3.kt
fun box(): String {
    "".k = C("K")
    return "O".k.x
}
