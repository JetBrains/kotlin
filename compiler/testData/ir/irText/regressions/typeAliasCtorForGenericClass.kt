class A<Q>(val q: Q)

typealias B<X> = A<X>

typealias B2<T> = A<A<T>>

fun bar() {
    val b = B(2)
    val b2 = B2(b)
}
