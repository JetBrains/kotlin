// MODULE: lib

// FILE: lib1.kt
// Exactly like `lib2.kt`, but the public function is called `use1`.
package lib

fun use1(): Int {
    return foo() + Any().foo()
}

private fun foo(arg: Any? = null) = 0
private fun Any.foo(arg: Any? = null) = 0

// FILE: lib2.kt
// Exactly like `lib1.kt`, but the public function is called `use2`.
package lib

fun use2(): Int {
    return foo() + Any().foo()
}

private fun foo(arg: Any? = null) = 0
private fun Any.foo(arg: Any? = null) = 0

// MODULE: main(lib)
// FILE: main.kt
import lib.*

fun box() = if (use1() + use2() == 0) "OK" else "FAIL"