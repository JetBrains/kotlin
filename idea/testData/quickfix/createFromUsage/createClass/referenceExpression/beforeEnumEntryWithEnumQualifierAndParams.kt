// "Create enum constant 'A'" "true"
// ERROR: No value passed for parameter n
package p

fun foo() = X.<caret>A

enum class X(n: Int) {

}