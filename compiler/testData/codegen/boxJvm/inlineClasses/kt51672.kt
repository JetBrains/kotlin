// MODULE: lib
// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: lib.kt
@JvmInline
value class S(val value: String)

interface A {
    fun f(s: S): S = s
}

interface B : A

// MODULE: main(lib)
// FILE: main.kt
interface C : B

fun box(): String {
    return object : C {}.f(S("OK")).value
}
