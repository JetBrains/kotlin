// IGNORE_BACKEND_FIR: JVM_IR
class Pair<T1, T2>(val x1: T1, val x2: T2)

typealias ST<T> = Pair<String, T>

fun box(): String {
    val st = ST<String>("O", "K")
    return st.x1 + st.x2
}