// FILE: B.kt

package b

abstract class MyClass

fun foo() {}

fun I() {}
interface I {}

// FILE: A.kt

package a
import b.MyClass as HisClass
import b.foo as foo2
import b.I as I2

class YourClass : HisClass()

fun bar() {
    foo2()
    I2()
}
