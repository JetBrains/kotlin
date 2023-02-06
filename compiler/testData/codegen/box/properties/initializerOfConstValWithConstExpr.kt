// KT-56023
// JS_IR:  IllegalStateException: CONST_VAL_WITH_NON_CONST_INITIALIZER: Const 'val' initializer should be a constant value at (7,18) in /lib.kt
// NATIVE: test fails as below in both modes -Pkotlin.internal.native.test.mode=TWO_STAGE_MULTI_MODULE, -Pkotlin.internal.native.test.mode=ONE_STAGE_MULTI_MODULE
//   error: const 'val' initializer should be a constant value
// IGNORE_BACKEND_K2: NATIVE, JS_IR

// MODULE: lib
// FILE: lib.kt
const val four = 2 + 2

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    if (four == 4)
        return "OK"
    else
        return four.toString()
}
