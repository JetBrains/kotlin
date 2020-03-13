//FILE: test.kt
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
// test.kt:3
// test.kt:4
// test.kt:8
// test.kt:11
// test.kt:8
// test.kt:11
// test.kt:8
// test.kt:9
// test.kt:11
// test.kt:11
// test.kt:4
// test.kt:5
