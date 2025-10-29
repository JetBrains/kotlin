// ISSUE: KT-72446
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ KT-70295: symbol 'kotlin.internal/SharedVariableBox|null[0]' appeared in 2.2.20-Beta1
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
