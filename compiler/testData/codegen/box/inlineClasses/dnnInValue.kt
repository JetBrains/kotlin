// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// ISSUE: KT-62906 (related)

// MODULE: lib
// FILE: lib.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T>(val x: T & Any)

// MODULE: test(lib)
// FILE: test.kt

fun box(): String {
    val a1 = A("O")
    val a2 = A<String?>("K")
    return a1.x + a2.x + foo("")
}

fun <F : Any> foo(arg: F): F {
    val a = A<F?>(arg)
    return a.x
}
