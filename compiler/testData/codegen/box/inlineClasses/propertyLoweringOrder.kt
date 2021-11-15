// WITH_STDLIB
// FILE: 1.kt

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val x: String)

fun accessProperty(y: B): A {
    y.a = A("OK")
    return y.a
}

// FILE: 2.kt

class B(var a: A)

fun box(): String = accessProperty(B(A("Fail"))).x
