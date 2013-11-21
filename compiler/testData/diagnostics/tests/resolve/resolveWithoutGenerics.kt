//If this test hangs, it means something is broken.
package c

class A {
    val testVal : A = A()
}

//inappropriate but participating in resolve functions
fun foo(a: A, <!UNUSED_PARAMETER!>b<!>: A, <!UNUSED_PARAMETER!>i<!>: Int) = a
fun foo(a: Any) = a
fun foo(a: A, <!UNUSED_PARAMETER!>b<!>: Any) = a
fun foo(a: A, <!UNUSED_PARAMETER!>b<!>: A, <!UNUSED_PARAMETER!>s<!>: String) = a
//appropriate function
fun foo(a: A, <!UNUSED_PARAMETER!>b<!>: A) = a

fun test(a: A) {
    //the problem occurs if there are nested function invocations to resolve (resolve for them is repeated now)
    //to copy this invocation many times (and to comment/uncomment inappropriate functions) to see the difference
    foo(foo(a, foo(a, foo(a, a.testVal))), a)
}