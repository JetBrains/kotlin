// MODULE: main
// FILE: main.kt
package test

val callable: Int = 1

fun callable(): Int = 2

fun callable(): Int = 3

fun unrelated1(): Int = 0

fun callable(): Int = 4

val callable: Int = 5

fun unrelated2(): Int = 0

// callable: test/callable
