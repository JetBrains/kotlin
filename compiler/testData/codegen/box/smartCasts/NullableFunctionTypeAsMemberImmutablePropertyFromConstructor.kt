// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-63705

fun box(): String {
    Klass({})
    return "OK"
}

class Klass(val func: (() -> Unit)?) {
    init {
        if (func != null) {
            func()
        }
    }
}
