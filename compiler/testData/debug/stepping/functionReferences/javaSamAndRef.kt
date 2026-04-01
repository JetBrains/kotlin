// FULL_JDK
// TARGET_BACKEND: JVM
// FILE: test.kt

import java.util.function.Function

fun strToStr(p:String):String = p +""

fun box(): String {

    Function<String, String>(::strToStr).apply("")

    Function<String, String>(::strToStr)
        .apply("")

    Function<String, String>(
        ::strToStr
    ).apply("")

    Function<String, String>(
        ::strToStr
    )
        .apply ("")

    return "OK"
}

// EXPECTATIONS JVM_IR

// test.kt:11 box
// test.kt:11 box$strToStr
// test.kt:7 strToStr
// test.kt:11 box$strToStr
// test.kt:11 box

// test.kt:13 box
// test.kt:14 box
// test.kt:13 box$strToStr$0
// test.kt:7 strToStr
// test.kt:13 box$strToStr$0
// test.kt:14 box

// test.kt:16 box
// test.kt:18 box
// test.kt:17 box$strToStr$1
// test.kt:7 strToStr
// test.kt:17 box$strToStr$1
// test.kt:18 box

// test.kt:20 box
// test.kt:23 box
// test.kt:21 box$strToStr$2
// test.kt:7 strToStr
// test.kt:21 box$strToStr$2
// test.kt:23 box

// test.kt:25 box
