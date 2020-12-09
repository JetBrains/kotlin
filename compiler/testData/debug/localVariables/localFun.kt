

//FILE: test.kt
fun foo() {
    val x = 1
    fun bar()  {
        val y = x
    }
    bar()
}

fun box() {
    foo()
}

// Local functions are compiled to private static functions on the class
// containing the local funtion. This has a number of consequences observable
// from a debugging perspective, for this test specifically:
//   - local functions do not figure in the LVT of the outer function, as they
//     are not instantiated.
//   - the _declaration_ of the local function does not figure in the byte code
//     of the outer function and hence, has no line number
//   - captures are treated differently, according to the implementation
//     strategy: on the IR, captures are passed as arguments to the static
//     function, on the old backend, they are added to fields on the lambda
//     object. Hence, for debugging purposes, captures are "just"
//     variables local to the function, and hence need no name mangling to
//     properly figure in the debugger.

// LOCAL VARIABLES
// test.kt:13 box:
// test.kt:5 foo:

// LOCAL VARIABLES JVM
// test.kt:6 foo: x:int=1:int

// LOCAL VARIABLES JVM
// test.kt:9 foo: x:int=1:int, $fun$bar$1:TestKt$foo$1=TestKt$foo$1
// LOCAL VARIABLES JVM_IR
// test.kt:9 foo: x:int=1:int

// LOCAL VARIABLES JVM
// test.kt:7 invoke:
// test.kt:8 invoke: y:int=1:int
// LOCAL VARIABLES JVM_IR
// test.kt:7 foo$bar: x:int=1:int
// test.kt:8 foo$bar: x:int=1:int, y:int=1:int

// LOCAL VARIABLES JVM
// test.kt:10 foo: x:int=1:int, $fun$bar$1:TestKt$foo$1=TestKt$foo$1
// test.kt:14 box:
// LOCAL VARIABLES JVM_IR
// test.kt:10 foo: x:int=1:int
// test.kt:14 box: