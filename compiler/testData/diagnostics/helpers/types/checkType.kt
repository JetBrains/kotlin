fun <T> checkSubtype(t: T) = t

class CheckTypeInv<T>
fun <E> CheckTypeInv<E>._() {}
infix fun <T> T.checkType(f: CheckTypeInv<T>.() -> Unit) {}
