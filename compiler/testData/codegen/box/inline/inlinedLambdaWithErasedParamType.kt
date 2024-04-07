class A<T>(val prop: T)

inline fun <T> A<T>.process(action: (T) -> Unit) {
    action(prop)
}

inline fun acceptInt(p: Int, action: (Int) -> Unit) {
    action(p)
}

fun box(): String {
    var x = 0
    A(1).process { acceptInt(it) { p -> x += p } }
    return ('N' + x).toString() + "K"
}
