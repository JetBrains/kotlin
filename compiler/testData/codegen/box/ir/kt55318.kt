// IGNORE_BACKEND_K1: WASM
// ISSUE: KT-55318
// NATIVE_STANDALONE
// PARTIAL_LINKAGE_MODE: ENABLE
// PARTIAL_LINKAGE_LOG_LEVEL: INFO
// ^^^ Test requires partial linking, though NativeCodegenBoxTestNoPLGenerated turns it off by default.
//     So, PARTIAL_LINKAGE_MODE must be enabled explicitly, and NATIVE_STANDALONE must be specified.

// MODULE: lib
// FILE: lib.kt
package repro

interface I<out InterfaceTP>

open class Super<SuperTP> {
    fun foo(i: I</*redundant*/out SuperTP>) {}
}

class Sub<SubTP>: Super<SubTP>() {
    /* override fun foo(i: I<out SubTP>) */
}

// MODULE: main(lib)
// FILE: main.kt

package repro

class User<UserTP> {
    val sub = Sub<UserTP>()
    fun foo(i: I<UserTP>) = sub.foo(i)
}

fun box(): String {
    return "OK"
}
