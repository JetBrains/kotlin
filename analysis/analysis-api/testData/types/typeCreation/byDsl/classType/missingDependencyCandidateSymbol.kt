// MODULE: base
// FILE: Foo.kt
package base

class Foo<T>

// MODULE: expansion(base)
// FILE: expansion.kt
package expansion

import base.Foo

val propertyWithWrongTypeArguments: Foo<Int, String> = TODO()

// MODULE: main(expansion)
// FILE: main.kt
package main

import expansion.propertyWithWrongTypeArguments

fun test() {
    p<caret_type>ropertyWithWrongTypeArguments.toString()
}
