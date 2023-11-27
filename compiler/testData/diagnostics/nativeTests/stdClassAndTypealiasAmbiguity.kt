// ISSUE: KT-63794
// FILE: main1.kt

import kotlin.*
import kotlin.jvm.*
import kotlin.native.concurrent.*
import kotlin.native.*

@<!DEPRECATION, TYPEALIAS_EXPANSION_DEPRECATION!>SharedImmutable<!>
@<!DEPRECATION!>ThreadLocal<!>
val x = 42

@Throws(Exception::class)
fun test() {}

// FILE: main2.kt

val a = Throws::class
val b = <!DEPRECATION, TYPEALIAS_EXPANSION_DEPRECATION!>SharedImmutable<!>::class
val c = <!DEPRECATION!>ThreadLocal<!>::class
