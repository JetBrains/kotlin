// !WITH_NEW_INFERENCE
// See also KT-7804 (Wrong type inference of kotlin.Any? was for 'a' without explicit type)
fun <T> foo(a: T) = a

class A

fun <T> test(v: T): T {
    val a = if (v !is A) {
        foo(v) <!USELESS_CAST!>as T<!>
    }
    else {
        v
    }

    val t: T = a
    return t
}

fun <T> test2(v: T): T {
    val a = if (v !is A) {
        foo(v) <!USELESS_CAST!>as T<!>
    }
    else {
        v as T
    }

    val t: T = a
    return t
}

fun <T> test3(v: T): T {
    val a = if (v !is A) {
        foo(v)
    }
    else {
        v
    }

    val t: T = a
    return t
}

fun <T> test4(v: T): T {
    val a: T = if (v !is A) {
        foo(v) <!USELESS_CAST!>as T<!>
    }
    else {
        v
    }

    val t: T = a
    return t
}

fun <T> test5(v: T): T {
    val a: T = if (v !is A) {
        foo(v)
    }
    else {
        v
    }

    val t: T = a
    return t
}
