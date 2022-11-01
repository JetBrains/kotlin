

//FILE: test.kt
fun foo() {
    fun bar()  {
    }
}

fun box() {
    foo()
}

// Local functions are compiled to private static functions on the class
// containing the local funtion. This has a number of consequences observable
// from a debugging perspective, for this test specically:
//   - local functions do not figure in the LVT of the outer function, as they
//     are not instantiated.
//   - the _declaration_ of the local function does not figure in the byte code
//     of the outer function and hence, has no line number

// EXPECTATIONS JVM
// test.kt:10 box:
// test.kt:5 foo:
// test.kt:7 foo: $fun$bar$1:TestKt$foo$1=TestKt$foo$1
// test.kt:11 box:

// EXPECTATIONS JVM_IR JS_IR
// test.kt:10 box:
// test.kt:7 foo:
// test.kt:11 box:
