// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: Ice.kt
package test.api

val ice: Int = 10

fun doIce() {}

internal val internalIce: Int = 10

internal fun internalDoIce() {}

private val privateIce: Int = 10

private fun privateDoIce() {}

class Ice

companion fun Ice.companionIce() {}
companion val Ice.companionIce = 2

companion internal fun Ice.companionInternalIce() {}
companion internal val Ice.companionInternalIce = 2

companion private fun Ice.companionPrivateIce() {}
companion private val Ice.companionPrivateIce = 2

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

class Bar

companion fun Bar.companionBar() {}
companion val Bar.companionBar = 2

companion internal fun Bar.companionInternalBar() {}
companion internal val Bar.companionInternalBar = 2

companion private fun Bar.companionPrivateBar() {}
companion private val Bar.companionPrivateBar = 2

// FILE: main.kt
// package: test
