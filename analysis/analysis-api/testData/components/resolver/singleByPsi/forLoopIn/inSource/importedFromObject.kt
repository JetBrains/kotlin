// FILE: lib.kt
package dependency

class Foo

interface MyInterface {
    operator fun Foo.iterator(): Iterator<Int> = TODO()
}

// FILE: main.kt
package test

import dependency.*

import test.MyObject.iterator

object MyObject : MyInterface

fun usage() {
    for (x <caret>in Foo()) {}
}
