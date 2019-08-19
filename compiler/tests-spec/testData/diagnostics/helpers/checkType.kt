fun <T> checkSubtype(t: T) = t
class CheckType<T>
fun <E> CheckType<E>.check() {}

infix fun <T> T.checkType(f: CheckType<T>.() -> Unit) {}
