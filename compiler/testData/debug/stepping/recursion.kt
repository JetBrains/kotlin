// FILE: test.kt
fun box() {
    val n = 3
    val k = foo(n)
}

fun foo(n :Int ) : Int {
    if (n == 1 || n == 0) {
        return 1
    }
    return foo(n-1) * n
}

// LINENUMBERS
// test.kt:3 box
// test.kt:4 box
// test.kt:8 foo
// test.kt:11 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:11 foo
// test.kt:11 foo
// test.kt:4 box
// test.kt:5 box
