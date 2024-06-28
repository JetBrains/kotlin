// MODULE: base
// FILE: base.kt
package serialization.fake_overrides

open class A {
    open fun qux() = "quxSuper"
    open fun tic() = "ticSuper"
}

// MODULE: move(base)
// FILE: move.kt
// VERSION: 1
package serialization.fake_overrides

open class X {
}

class Y: X() {
    fun bar() = "barStale"
}

class B: A() {
}

class C: A() {
    override fun tic() = "ticChild"
}

// FILE: move2.kt
// VERSION: 2
package serialization.fake_overrides

open class X {
    fun bar() = "barMoved"
}

class Y: X() {
}

class B: A() {
    override fun qux() = "quxChild"
}

class C: A() {
}

// MODULE: use(move, base)
// FILE: use.kt
package serialization.fake_overrides

class Z: X() {
}

fun test0() = Y().bar()
fun test2() = B().qux()
fun test3() = C().qux()
fun test4() = B().tic()
fun test5() = C().tic()

// MODULE: main(use, move, base)
// FILE: main.kt
import serialization.fake_overrides.*
fun test1() = Z().bar()

fun box(): String {
    val failedTests = listOfNotNull(
        test0().takeIf { it != "barMoved" },
        test1().takeIf { it != "barMoved" },
        test2().takeIf { it != "quxChild" },
        test3().takeIf { it != "quxSuper" },
        test4().takeIf { it != "ticSuper" },
        test5().takeIf { it != "ticSuper" },
    )
    return if (failedTests.isNotEmpty()) failedTests.joinToString() else "OK"
}
