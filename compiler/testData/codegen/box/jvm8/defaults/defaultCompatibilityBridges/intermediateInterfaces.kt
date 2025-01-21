// TARGET_BACKEND: JVM
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface A {
    fun f(): String = "Fail"
}

interface AB1 : A
interface AB2 : A
interface AB3 : AB1, AB2

open class B : AB3

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: source.kt
import base.*

interface AC1 : A
interface AC2 : A
interface AC3 : AC1, AC2

interface C : AC3 {
    override fun f(): String = "OK"
}

interface CD1 : C
interface CD2 : C
interface CD3 : CD1, CD2

class D : B(), CD3

fun box(): String = D().f()
