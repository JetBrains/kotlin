// ISSUE: KT-72446
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization
// MODULE: lib
// FILE: lib.kt
inline fun test(
    block: () -> String = {
        var result = "O"
        val temp = { result += "K" }
        temp()
        result
    }
) = block()

// MODULE: main(lib)
// FILE: main.kt
fun box() = test()
