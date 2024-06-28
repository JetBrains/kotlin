// FILE: main.kt
package test

import dependency.B
import dependency.C
import dependency.bar
import dependency.Foo
import dependency.zoo
import dependency.Bar
import dependency.b
import dependency.Base
import dependency.Child
import dependency.ext
import dependency.AnotherClass

/**
 * [dependency.A]
 * [dependency.C]
 * [B]
 * [bar]
 * [zoo]
 * [Bar.b]
 * [Child.ext]
 * [AnotherClass.foo]
 */
fun foo() {}

// FILE: dependency.kt
package dependency

class A
class B
fun bar() {}
class Foo
fun Foo.zoo() {}
class Bar
fun Bar.b(){}

open class Base
class Child : Base()
fun Base.ext() {}

class AnotherClass {
    fun foo(){}
}