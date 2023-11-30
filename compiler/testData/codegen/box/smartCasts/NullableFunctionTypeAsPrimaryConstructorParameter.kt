fun box(): String {
    Klass({})
    return "OK"
}

class Klass(func: (() -> Unit)?) {
    init {
        if (func != null) {
            func()
        }
    }
}
