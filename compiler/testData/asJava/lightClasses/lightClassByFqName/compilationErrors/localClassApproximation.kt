// p.B
package p

interface Self<E>

class B {
    val x = run {
        class A : Self<A>
        A()
    }

    val y = B().x
}