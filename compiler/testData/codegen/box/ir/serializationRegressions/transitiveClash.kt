// IGNORE_BACKEND_FIR: JVM_IR
// EXPECTED_REACHABLE_NODES: 1304
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
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