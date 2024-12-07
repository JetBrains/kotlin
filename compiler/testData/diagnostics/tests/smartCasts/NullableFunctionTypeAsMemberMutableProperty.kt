// RUN_PIPELINE_TILL: FRONTEND
fun box(): String {
    KlassA({})
    KlassB()
    return "OK"
}

class KlassA(arg: (() -> Unit)?) {
    var func: (() -> Unit)? = arg
    init {
        if (func != null) {
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>func<!>()
        }
    }
}

class KlassB {
    var func: (() -> Unit)?
    init {
        if (true) {
            func = {}
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>func<!>()
        } else {
            func = null
        }
    }
}
