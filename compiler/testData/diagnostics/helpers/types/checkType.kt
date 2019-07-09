package tests._checkType

fun <T> checkSubtype(t: T) = t
class Inv<T>
fun <E> Inv<E>._() {}

infix fun <T> T.checkType(f: Inv<T>.() -> Unit) {}
