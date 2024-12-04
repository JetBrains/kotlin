// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// ISSUE: KT-73202

annotation class Ann

class A {
    val a: Any = ""

    @get:Ann
    val b: Any = ""

    val c: Any = ""
        @Ann get

    val d: Any
        get() = ""

    val e: Any by lazy { "" }
}

fun test(a: A) {
    if (a.a is String) a.a.length
    if (a.b is String) <!SMARTCAST_IMPOSSIBLE!>a.b<!>.length
    if (a.c is String) <!SMARTCAST_IMPOSSIBLE!>a.c<!>.length
    if (a.d is String) <!SMARTCAST_IMPOSSIBLE!>a.d<!>.length
    if (a.e is String) <!SMARTCAST_IMPOSSIBLE!>a.e<!>.length
}
