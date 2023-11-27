// ISSUE: KT-63794
// FILE: main1.kt

import kotlin.*
import kotlin.jvm.*
import kotlin.native.concurrent.*
import kotlin.native.*

@<!DEPRECATION!>SharedImmutable<!>
@ThreadLocal
val x = 42

@Throws(Exception::class)
fun test() {}

// FILE: main2.kt

val a = Throws::class
val b = <!DEPRECATION!>SharedImmutable<!>::class
val c = <!DEPRECATION!>ThreadLocal<!>::class