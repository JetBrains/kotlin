// FILE: classes.kt
package classes

abstract class Foo
abstract class Bar

// FILE: bar.kt
package classes.bar

import classes.Bar

inline fun <reified T : Bar> nameOf(): String {
   return "Bar"
}

// FILE: foo.kt
package classes.foo

import classes.Foo

inline fun <reified T : Foo> nameOf(): String {
    return "Foo"
}

// FILE: main.kt
package main

import classes.*
import classes.bar.*
import classes.foo.*

fun <T> foo() {}

fun test() {
   nameOf<Bar>()
   nameOf<Foo>()
}
