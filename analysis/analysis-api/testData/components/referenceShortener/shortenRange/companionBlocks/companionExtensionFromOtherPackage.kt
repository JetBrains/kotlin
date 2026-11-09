// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
package test

import dependency.foo

class Owner

fun bar(): Int = <expr>test.Owner.foo()</expr>

// FILE: dependency.kt
package dependency

import test.Owner

companion fun Owner.foo(): Int = 1
