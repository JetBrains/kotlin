// "Create enum constant 'A'" "false"
// ACTION: Convert to block body
// ACTION: Rename reference
// ERROR: Unresolved reference: A
package p

fun foo(): X = E.<caret>A

enum class E {

}

open class X {

}