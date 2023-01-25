// FILE: main.kt
package my.simple.name

fun one() {}

open class Parent {
    fun one() {}
}

class Child: Parent() {
    fun test() {
        <expr>my.simple.name.one()</expr>
    }
}