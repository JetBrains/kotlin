// TARGET_BACKEND: WASM
// RUN_THIRD_PARTY_OPTIMIZER

// MODULE: lib
// FILE: externals.js

function foo(f) {
    return f(4, 5);
}

// FILE: lib.kt

external fun foo(f: (Int, Int) -> Int): Int

fun runLibClosureCallHelperTest(): String? {
    if (foo { x, y -> x + y } != 9) return "foo"
    return null
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String =
    runLibClosureCallHelperTest()?.let { "Fail: $it" } ?: "OK"

// FILE: entry.mjs
import * as generated from "./index.mjs"

const expectsDependencyModule = Object.hasOwn(generated, "__ALL_EXPORTS")
let libGenerated
try {
    libGenerated = await import("./_lib_.mjs")
} catch (e) {
    if (expectsDependencyModule) {
        throw Error(`Dependency module should be generated when raw main exports are visible: ${e}`)
    }
}

function assertNoCallFunctionExports(namespaceName, namespace) {
    const leaked = Object.keys(namespace).filter((name) => name.startsWith("__callFunction_"))
    if (leaked.length !== 0) {
        throw Error(`${namespaceName} leaks __callFunction_* exports: ${leaked.join(", ")}`)
    }
}

if (generated.box() !== "OK") {
    throw Error("Wrong box result")
}

assertNoCallFunctionExports("main named exports", generated)

if (expectsDependencyModule) {
    if (!Object.hasOwn(libGenerated, "__ALL_EXPORTS")) {
        throw Error("lib should export __ALL_EXPORTS")
    }
    assertNoCallFunctionExports("lib raw exports", libGenerated.__ALL_EXPORTS)
    assertNoCallFunctionExports("lib named exports", libGenerated)
} else if (libGenerated !== undefined) {
    throw Error("Regular mode should not generate a separate lib module")
}
