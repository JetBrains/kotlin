package test

// a

val t = "a"

fun foo(/*rename*/a: Int) {
    // a
    val tt = "a"
}

fun test() = foo(a = 1)