// IGNORE_BACKEND_FIR: JVM_IR
class Wrapper<T>(var x: T)

inline fun <reified T> change(w: Wrapper<T>, x: Any?) {
    if (x is T) {
        w.x = x
    }
}

fun box(): String {
    val w = Wrapper<String>("FAIL")
    change(w, "OK")
    return w.x
}
