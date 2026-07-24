// TARGET_BACKEND: WASM
// RUN_THIRD_PARTY_OPTIMIZER

// MODULE: lib
// FILE: externals.js

function foo(f) {
    return f(4, 5);
}

// FILE: lib.kt

external fun foo(f: (Int, Int) -> Int): Int

fun noKotlinClosureCallHelpersExportedFromLib(): Boolean =
    js("Object.keys(wasmExports).every((key) => !key.startsWith('__callFunction_'))")

fun runLibClosureCallHelperTest(): String? {
    if (foo { x, y -> x + y } != 9) return "foo"
    if (!noKotlinClosureCallHelpersExportedFromLib()) return "__callFunction_* helper leaked from lib wasmExports"
    return null
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String =
    runLibClosureCallHelperTest()?.let { "Fail: $it" } ?: "OK"
