// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: 1.kt
package a

class someFun() {}
fun someFun(i: Int) {}

class someVal() {}
val Int.someVal: Int get() = 3

class A

class B

// FILE: 2.kt
package b

class someFun
class someVal
class someAll

fun A() {}

class B


// FILE: 3.kt
import a.someFun
import b.someFun

import a.someVal
import b.someVal

import a.A
import b.A

// FILE: 4.kt
import b.*
import a.B

// FILE: 5.kt
package b

import a.B

// FILE: 6.kt
import a.B
import b.B