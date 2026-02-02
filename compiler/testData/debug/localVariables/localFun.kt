

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

// EXPECTATIONS WASM
// test.kt:13 $box: (4)
// test.kt:5 $foo: $x:i32=0:i32 (12)
// test.kt:9 $foo: $x:i32=1:i32 (4, 4)
// test.kt:7 $foo$bar: $x:i32=1:i32, $y:i32=0:i32 (16, 16)
// test.kt:8 $foo$bar: $x:i32=1:i32, $y:i32=1:i32 (5, 5)
// test.kt:10 $foo: $x:i32=1:i32 (1, 1)
// test.kt:14 $box: (1)
