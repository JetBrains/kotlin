// FILE: test.kt

fun foo(block: (String) -> String) = block("")

fun strToStr(p:String):String = p +""

fun box(): String {

    foo(::strToStr)

    foo (
        ::strToStr
    )

    return "OK"
}

// EXPECTATIONS JVM_IR

// test.kt:9 box
// test.kt:3 foo
// test.kt:9 invoke
// test.kt:5 strToStr
// test.kt:9 invoke
// test.kt:3 foo
// test.kt:9 box

// test.kt:12 box
// test.kt:11 box
// test.kt:3 foo
// test.kt:12 invoke
// test.kt:5 strToStr
// test.kt:12 invoke
// test.kt:3 foo
// test.kt:11 box

// test.kt:15 box

// EXPECTATIONS WASM
// test.kt:9 $box (4)
// test.kt:3 $foo (37, 43, 37)
// test.kt:9 $strToStr$ref.invoke (8)
// test.kt:5 $strToStr (32, 35, 32, 37)
// test.kt:9 $strToStr$ref.invoke (8)
// test.kt:3 $foo (37, 46)
// test.kt:9 $box (4)

// test.kt:11 $box (4)
// test.kt:3 $foo (37, 43, 37)
// test.kt:12 $strToStr$ref.invoke (8)
// test.kt:5 $strToStr (32, 35, 32, 37)
// test.kt:12 $strToStr$ref.invoke (8)
// test.kt:3 $foo (37, 46)
// test.kt:11 $box (4)

// test.kt:15 $box (11, 4)

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:9 strToStr$ref
// test.kt:9 box
// test.kt:3 foo
// test.kt:5 strToStr

// test.kt:11 box
// test.kt:12 strToStr$ref
// test.kt:11 box
// test.kt:3 foo
// test.kt:5 strToStr

// test.kt:15 box

// EXPECTATIONS NATIVE
// test.kt:9 box
// test.kt:3 foo
// test.kt:9 invoke
// test.kt:5 strToStr
// test.kt:5 strToStr
// test.kt:9 invoke
// test.kt:3 foo
// test.kt:9 box

// test.kt:11 box
// test.kt:3 foo
// test.kt:12 invoke
// test.kt:5 strToStr
// test.kt:5 strToStr
// test.kt:12 invoke
// test.kt:3 foo
// test.kt:11 box

// test.kt:15 box
// test.kt:16 box
