// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization, +IrCrossModuleInlinerBeforeKlibSerialization
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM, NATIVE
// ^^^KT-80653 Internal error in body lowering: java.lang.IllegalStateException: Local declarations should've been popped out by this point
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
inline fun <R> myRun(block: () -> R): R {
    return block()
}

// FILE: main.kt
fun box() = myRun {
    object {
        fun foo(): String {
            fun localFun() = "OK"
            return localFun()
        }
    }.foo()
}
