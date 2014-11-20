// "Create class 'B'" "true"
// ERROR: Unresolved reference: C
package p

fun foo() = A.B.C

class A {
    class B {

    }

}