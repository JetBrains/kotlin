fun box(): String {
    Klass({})
    return "OK"
}

class Klass(var func: (() -> Unit)?) {
    init {
        if (func != null) {
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>func<!>()
        }
    }
}
