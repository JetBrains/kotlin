// WITH_STDLIB
// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR

// FILE: a.kt

@JvmInline
value class IC(val x: String)

class C(val ic: IC)

// FILE: b.kt

fun foo(action: (ic: IC) -> C): C {
    return action(IC("OK"))
}

fun test(): C {
    return foo(::C)
}

// FILE: c.kt

fun box(): String {
    return test().ic.x
}
