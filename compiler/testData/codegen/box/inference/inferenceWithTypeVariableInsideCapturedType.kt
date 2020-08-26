class Inv<T>

fun <T : V, U : V, V> foo(x: T, y: Inv<in U>) {}

fun <E> materializeInvInv(): Inv<in Inv<E>?> = Inv()

fun test(inv: Inv<Int>) {
    foo(inv, materializeInvInv())
}

fun box(): String {
    test(Inv<Int>())
    return "OK"
}