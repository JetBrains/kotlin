// FILE: test.kt

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Anno

fun box() {
    @Anno
    var x: Any = 1

    @Anno
    lateinit var y: Any

    x = 2
    x

    y = 2
    y
}

// EXPECTATIONS JVM_IR
// test.kt:8 box
// test.kt:11 box
// test.kt:13 box
// test.kt:14 box
// test.kt:16 box
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS JS_IR FIR
// test.kt:8 box
// test.kt:11 box
// test.kt:13 box
// test.kt:16 box
// test.kt:17 box
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS WASM FIR
// test.kt:8 $box (17)
// test.kt:11 $box (13)
// test.kt:13 $box (8)
// test.kt:14 $box (4)
// test.kt:16 $box (8)
// test.kt:17 $box (4)
// test.kt:18 $box (1)
