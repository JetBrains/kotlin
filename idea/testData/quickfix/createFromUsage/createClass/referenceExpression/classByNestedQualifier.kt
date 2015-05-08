// "Create class 'B'" "true"
// ERROR: Unresolved reference: C
package p

fun foo() = A.<caret>B.C

class A {

}