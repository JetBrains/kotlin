fun box(): String {
    KlassA({})
    KlassB()
    return "OK"
}

class KlassA(arg: (() -> Unit)?) {
    var func: (() -> Unit)? = arg
    init {
        if (func != null) {
            <!SMARTCAST_IMPOSSIBLE!>func<!>()
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
