// FIR_IDENTICAL
//If this test hangs, it means something is broken.
package a

class A {
    val testVal : A = A()
}

//inappropriate but participating in resolve functions
fun <T> foo(a: T, b: T, i: Int) = a
fun foo(a: Any) = a
fun <T> foo(a: T, b: String) = a
fun <T> foo(a: T, b: T, s: String) = a
//appropriate function
fun <T> foo(a: T, b: T) = a

fun test(a: A) {
    //the problem occurs if there are nested function invocations to resolve (resolve for them is repeated now)
    //to copy this invocation many times (and to comment/uncomment inappropriate functions) to see the difference
    foo(foo(a, foo(a, foo(a, a.testVal))), a)

}
