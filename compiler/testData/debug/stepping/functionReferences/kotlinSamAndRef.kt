// FILE: test.kt

fun strToStr(p:String):String = p +""

fun interface FunInterface {
    fun bar(p: String): String
}

fun box(): String {

    FunInterface(::strToStr).bar("")

    FunInterface(::strToStr)
        .bar("")

    FunInterface(
        ::strToStr
    ).bar("")

    FunInterface(
        ::strToStr
    )
        .bar("")

    return "OK"
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:11 bar
// test.kt:3 strToStr
// test.kt:11 bar
// test.kt:11 box

// test.kt:13 box
// test.kt:14 box
// test.kt:13 bar
// test.kt:3 strToStr
// test.kt:13 bar
// test.kt:14 box

// test.kt:16 box
// test.kt:18 box
// test.kt:17 bar
// test.kt:3 strToStr
// test.kt:17 bar
// test.kt:18 box

// test.kt:20 box
// test.kt:23 box
// test.kt:21 bar
// test.kt:3 strToStr
// test.kt:21 bar
// test.kt:23 box

// test.kt:25 box

// EXPECTATIONS WASM
// test.kt:11 $box (17, 29, 33, 29)
// test.kt:11 $strToStr$ref.invoke (17)
// test.kt:3 $strToStr (32, 35, 32, 37)
// test.kt:11 $strToStr$ref.invoke (17)
// test.kt:11 $box (29)

// test.kt:13 $box (17)
// test.kt:14 $box (9, 13, 9)
// test.kt:13 $strToStr$ref.invoke (17)
// test.kt:3 $strToStr (32, 35, 32, 37)
// test.kt:13 $strToStr$ref.invoke (17)
// test.kt:14 $box (9)

// test.kt:17 $box (8)
// test.kt:18 $box (6, 10, 6)
// test.kt:17 $strToStr$ref.invoke (8)
// test.kt:3 $strToStr (32, 35, 32, 37)
// test.kt:17 $strToStr$ref.invoke (8)
// test.kt:18 $box (6)

// test.kt:21 $box (8)
// test.kt:23 $box (9, 13, 9)
// test.kt:21 $strToStr$ref.invoke (8)
// test.kt:3 $strToStr (32, 35, 32, 37)
// test.kt:21 $strToStr$ref.invoke (8)
// test.kt:23 $box (9)

// test.kt:25 $box (11, 4)

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:11 strToStr$ref
// test.kt:11 box
// test.kt:11 <init>
// test.kt:11 <init>
// test.kt:11 box
// test.kt:3 strToStr

// test.kt:13 box
// test.kt:13 strToStr$ref
// test.kt:14 box
// test.kt:11 <init>
// test.kt:11 <init>
// test.kt:14 box
// test.kt:3 strToStr

// test.kt:17 box
// test.kt:17 strToStr$ref
// test.kt:18 box
// test.kt:11 <init>
// test.kt:11 <init>
// test.kt:18 box
// test.kt:3 strToStr

// test.kt:21 box
// test.kt:21 strToStr$ref
// test.kt:23 box
// test.kt:11 <init>
// test.kt:11 <init>
// test.kt:23 box
// test.kt:3 strToStr

// test.kt:25 box

// EXPECTATIONS NATIVE
// test.kt:11 box
// test.kt:11 bar
// test.kt:3 strToStr
// test.kt:3 strToStr
// test.kt:11 bar
// test.kt:11 box

// test.kt:14 box
// test.kt:13 bar
// test.kt:3 strToStr
// test.kt:3 strToStr
// test.kt:13 bar
// test.kt:14 box

// test.kt:18 box
// test.kt:16 bar
// test.kt:17 bar
// test.kt:3 strToStr
// test.kt:3 strToStr
// test.kt:17 bar
// test.kt:18 bar
// test.kt:18 box

// test.kt:23 box
// test.kt:20 bar
// test.kt:21 bar
// test.kt:3 strToStr
// test.kt:3 strToStr
// test.kt:21 bar
// test.kt:22 bar
// test.kt:23 box

// test.kt:25 box
// test.kt:26 box
