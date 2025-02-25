// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun <T> foo(a: T) = a

context(a: T)
fun <T> foo2() = a

fun <T> T.foo3() = this

fun <T> T.foo4(a: T) = a

context(a: T)
fun <T> T.foo5(a2: T) = a

fun <T> T.test(): T {
    return foo(this) <!USELESS_CAST!>as T<!>
}

fun <T> T.test2(): T {
    return foo2() <!USELESS_CAST!>as T<!>
}

fun <T> T.test3(): T {
    return foo3() <!USELESS_CAST!>as T<!>
}

fun <T> T.test4(): T {
    return foo4(this) <!USELESS_CAST!>as T<!>
}

fun <T> T.test5(): T {
    return foo5(this) <!USELESS_CAST!>as T<!>
}