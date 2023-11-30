fun box(): String {
    Klass({})
    return "OK"
}

class Klass(var func: (() -> Unit)?) {
    init {
        if (func != null) {
            <!SMARTCAST_IMPOSSIBLE!>func<!>()
        }
    }
}
