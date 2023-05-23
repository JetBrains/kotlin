// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, NATIVE, WASM
// ISSUE: KT-55318
// KT-55486: native linker error with `-Pkotlin.internal.native.test.cacheMode=STATIC_EVERYWHERE`
// IGNORE_BACKEND_K2: NATIVE

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
