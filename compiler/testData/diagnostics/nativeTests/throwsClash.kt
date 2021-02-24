// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: throws.kt
package kotlin

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
public annotation class Throws(vararg val ThrowableClasses: KClass<out Throwable>)

// FILE: native.kt
package kotlin.native

@Deprecated("")
public typealias Throws = kotlin.Throws

// FILE: main1.kt
package abc1

@Throws(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main2.kt
package abc2

import kotlin.native.<!DEPRECATION!>Throws<!>

@<!DEPRECATION!>Throws<!>(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: <!DEPRECATION!>Throws<!>) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main3.kt
package abc3

import kotlin.Throws

@Throws(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main4.kt
package abc4

import kotlin.<!CONFLICTING_IMPORT!>Throws<!>
import kotlin.native.<!CONFLICTING_IMPORT, DEPRECATION!>Throws<!>

@Throws(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main5.kt
package abc5

import kotlin.native.*

@<!DEPRECATION!>Throws<!>(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: <!DEPRECATION!>Throws<!>) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main6.kt
package abc6

import kotlin.*

@Throws(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main7.kt
package abc7

import kotlin.*
import kotlin.native.*

@Throws(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main8.kt
package abc8

import kotlin.*
import kotlin.native.<!DEPRECATION!>Throws<!>

@<!DEPRECATION!>Throws<!>(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: <!DEPRECATION!>Throws<!>) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}

// FILE: main9.kt
package abc9

import kotlin.native.*
import kotlin.Throws

@Throws(Throwable::class)
fun foo1() {}

@kotlin.Throws(Throwable::class)
fun foo2() {}

@<!DEPRECATION!>kotlin.native.Throws<!>(Throwable::class)
fun foo3() {}

fun foo5(x: Throws) {}
fun foo6(x: kotlin.Throws) {}
fun foo7(x: kotlin.native.<!DEPRECATION!>Throws<!>) {}
