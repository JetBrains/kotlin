// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: a.kt
package a

import test.C

companion fun C.foo() {}

// FILE: b.kt
package b

import test.C

companion fun C.foo() {}

// FILE: main.kt
package test

import a.foo
import b.foo

class C

fun usage() {
    C.foo()
}
