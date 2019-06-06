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
// TestKt.box():3
// TestKt.box():4
// TestKt.foo(int):8
// TestKt.foo(int):11
// TestKt.foo(int):8
// TestKt.foo(int):11
// TestKt.foo(int):8
// TestKt.foo(int):9
// TestKt.foo(int):11
// TestKt.foo(int):11
// TestKt.box():4
// TestKt.box():5
