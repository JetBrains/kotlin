// KT-56023
// JS_IR:  InterpreterError: Unsupported number of arguments for invocation as builtin function: four
// NATIVE: in mode -Pkotlin.internal.native.test.mode=TWO_STAGE_MULTI_MODULE (default test mode), test fails as below
//   InterpreterError: Unsupported number of arguments for invocation as builtin function: four
//         in mode -Pkotlin.internal.native.test.mode=ONE_STAGE_MULTI_MODULE, test passes
// IGNORE_BACKEND_K2: NATIVE, JS_IR

// MODULE: lib
// FILE: lib.kt
const val four = 4

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    if (four == 4)
        return "OK"
    else
        return four.toString()
}
