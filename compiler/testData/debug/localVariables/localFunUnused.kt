

//FILE: test.kt
fun foo() {
    fun bar()  {
    }
}

fun box() {
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:10 box:
// test.kt:7 foo:
// test.kt:11 box:

// EXPECTATIONS WASM
// test.kt:10 $box: (4)
// test.kt:7 $foo: (1)
// test.kt:11 $box: (1)
