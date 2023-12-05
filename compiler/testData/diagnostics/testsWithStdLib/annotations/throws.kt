// FIR_IDENTICAL
// ISSUE: KT-63794
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: main1.kt
package abc1

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main2.kt
package abc2

import kotlin.jvm.Throws

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main3.kt
package abc3

import kotlin.Throws

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main4.kt
package abc4

import kotlin.<!CONFLICTING_IMPORT!>Throws<!>
import kotlin.jvm.<!CONFLICTING_IMPORT!>Throws<!>

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main5.kt
package abc5

import kotlin.jvm.*

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main6.kt
package abc6

import kotlin.*

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main7.kt
package abc7

import kotlin.*
import kotlin.jvm.*

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main8.kt
package abc8

import kotlin.*
import kotlin.jvm.Throws

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main9.kt
package abc9

import kotlin.jvm.*
import kotlin.Throws

@Throws(Exception::class)
fun foo1() {}

@kotlin.Throws(Exception::class)
fun foo2() {}

@kotlin.jvm.Throws(Exception::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.jvm.Throws) {}

// FILE: main10.kt

val x: Throws? = null
val y = Throws()
val z = Throws::exceptionClasses
val w = Throws::class