// WITH_STDLIB
// KT-90639

// MODULE: lib
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// FILE: wrapped.kt
fun defaultValue(): Int = 7
inline fun withDefault(block: () -> Int = {
    defaultValue()
}): Int {
    return block()
}

// MODULE: main(lib)
// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// FILE: default.kt
fun defaultCall(): Int {
    return withDefault()
}

fun explicitCall(x: Int): Int {
    return withDefault {
        x + 2
    }
}

// FILE: test.kt
fun box(): String = if (defaultCall() == 7 && explicitCall(1) == 3) "OK" else "FAIL"

// EXPECTATIONS NATIVE
// default.kt:17 defaultCall
// default.kt:18 defaultCall
// wrapped.kt:11 defaultCall
// wrapped.kt:9 defaultCall
// wrapped.kt:7 defaultValue
// wrapped.kt:9 defaultCall
// wrapped.kt:11 defaultCall
// default.kt:18 defaultCall
// default.kt:19 defaultCall
// test.kt:28 box
// default.kt:21 explicitCall
// default.kt:22 explicitCall
// wrapped.kt:11 explicitCall
// default.kt:23 explicitCall
// wrapped.kt:11 explicitCall
// default.kt:24 explicitCall
// default.kt:25 explicitCall
// test.kt:28 box

// EXPECTATIONS JS_IR
// test.kt:28 box
// default.kt:18 defaultCall
// wrapped.kt:7 defaultValue
// test.kt:28 box
// default.kt:22 explicitCall

// EXPECTATIONS WASM
// test.kt:28 $box (24)
// default.kt:18 $defaultCall (11)
// wrapped.kt:11 $defaultCall (11)
// wrapped.kt:9 $defaultCall (4)
// wrapped.kt:7 $defaultValue (26, 27)
// wrapped.kt:9 $defaultCall (18)
// wrapped.kt:11 $defaultCall (4)
// default.kt:18 $defaultCall (4)
// test.kt:28 $box (41, 24, 59, 46)
// default.kt:22 $explicitCall (11)
// wrapped.kt:11 $explicitCall (11)
// default.kt:23 $explicitCall (8, 12, 8, 13)
// wrapped.kt:11 $explicitCall (4)
// default.kt:22 $explicitCall (4)
// test.kt:28 $box (65, 46, 68, 84)
