// ISSUE: KT-55318
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 1.9.20
// ^^^KT-55318 is fixed in 2.0.0-Beta1

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
