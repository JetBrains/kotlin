

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

// EXPECTATIONS JVM_IR
// test.kt:13 box:
// test.kt:5 foo:
// test.kt:9 foo: x:int=1:int
// test.kt:7 foo$bar: x:int=1:int
// test.kt:8 foo$bar: x:int=1:int, y:int=1:int
// test.kt:10 foo: x:int=1:int
// test.kt:14 box:

// EXPECTATIONS JS_IR
// test.kt:13 box:
// test.kt:5 foo:
// test.kt:9 foo: x=1:number
// test.kt:7 foo$bar: x=1:number
// test.kt:8 foo$bar: x=1:number, y=1:number
// test.kt:10 foo: x=1:number
// test.kt:14 box:
