// DO_NOT_CHECK_SYMBOL_RESTORE_K1

// FILE: Ice.kt
package test.api

val ice: Int = 10

fun doIce() {}

internal val internalIce: Int = 10

internal fun internalDoIce() {}

private val privateIce: Int = 10

private fun privateDoIce() {}

// FILE: Foo.kt
package test

import test.api.*

val foo: Int = ice + 32

fun doFoo() {
    doIce()
}

internal val internalFoo: Int = ice + 32

internal fun internalDoFoo() {
    internalDoIce()
}

private val privateFoo: Int = ice + 32

private fun privateDoFoo() {}

// FILE: Bar.kt
package test

val bar: Int = ice - 8

fun doBar() {
    doIce()
}

internal val internalBar: Int = ice - 8

internal fun internalDoBar() {
    internalDoIce()
}

private val privateBar: Int = ice - 8

private fun privateDoBar() {}

// FILE: main.kt
// package: test
