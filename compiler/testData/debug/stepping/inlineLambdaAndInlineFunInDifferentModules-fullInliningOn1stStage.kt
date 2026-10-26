// WITH_STDLIB
// KT-90639

// MODULE: lib
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// FILE: wrapped.kt
inline fun wrapped(block: () -> Int): Int =
    1.let {
        block() + it
    }

// MODULE: main(lib)
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// FILE: test.kt
fun box(): String {
    val result = wrapped {
        2
    }
    return if (result == 3) "OK" else "fail"
}

// EXPECTATIONS NATIVE
// test.kt:16 box
// wrapped.kt:8 box
// wrapped.kt:9 box
// test.kt:17 box
// wrapped.kt:9 box
// wrapped.kt:10 box
// test.kt:18 box
// test.kt:19 box
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:17 box
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:16 $box (17)
// wrapped.kt:8 $box (4, 6)
// wrapped.kt:9 $box (8)
// test.kt:17 $box (8, 9)
// wrapped.kt:9 $box (18, 8, 20)
// wrapped.kt:10 $box (5)
// test.kt:19 $box (15, 25, 15, 28, 4)
