// "Create enum 'A'" "true"
// ERROR: Unresolved reference: B
package p

fun foo() = A.B

enum class A {

}
