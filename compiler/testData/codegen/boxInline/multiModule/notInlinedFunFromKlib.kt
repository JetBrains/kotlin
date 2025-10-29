// SKIP_IR_DESERIALIZATION_CHECKS
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ IrReturnableBlockImpl serialization is not supported at ABI compatibility level 2.2:
// DUMP_IR_AFTER_INLINE
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

inline fun <T> foo(arg: T, block: (T) -> Unit) {
    block(arg)
}

// MODULE: main(lib)
// FILE: main.kt

inline fun <T> inner(arg: T, block: (T) -> Unit) {
    foo(arg, block)
}

inline fun <T> wrap(arg: T, block: (T) -> Unit) {
    inner(arg, block)
}

fun box(): String {
    var x = "fail"
    wrap("OK") {
        foo(it) {
            x = it
        }
    }

    return x
}
