// "Create enum constant 'A'" "false"
// ACTION: Convert to block body
// ACTION: Rename reference
// ACTION: Create extension property 'E.Companion.A'
// ACTION: Create member property 'E.Companion.A'
// ERROR: Unresolved reference: A
package p

fun foo(): X = E.<caret>A

enum class E {

}

open class X {

}