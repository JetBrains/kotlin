// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: SERIALIZATION_REGRESSION: Conflicting overloads: public fun foo(): String defined in pkg in file lib1.kt, public fun foo(): String defined in pkg in file main.kt (6,1)
// TODO(KT-86166): This test fails in Wasm monolith mode, but not in single module mode.
// With the current test infra, it's not possible to express this, so the test is hard-ignored for single module mode by excluding it during test generation.
// Once KT-86166 is solved and provides us tools to exclude it properly, do that.
// JS_IR error: Cross module dependency resolution failed due to signature 'pkg/foo|-1041209573719867811[0]' redefinition
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// NATIVE error caused by `foo()` clash: kotlin.AssertionError: Test failed with: 42K. Expected <OK>, actual <42K>.
// DONT_TARGET_EXACT_BACKEND: NATIVE

// MODULE: lib1
// FILE: lib1.kt
package pkg

fun foo(): String { return "O" }

// MODULE: lib2(lib1)
// FILE: lib2.kt
package pkg

fun bar(): String { return foo() + "K" }

// MODULE: main(lib2)
// FILE: main.kt

package pkg

fun foo(): String { return "42" }

fun box(): String {

    if (foo() != "42") return "FAIL: ${foo()}"

    return bar()
}
