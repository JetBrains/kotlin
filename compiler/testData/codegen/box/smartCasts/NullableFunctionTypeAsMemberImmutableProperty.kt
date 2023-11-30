// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-63705

fun box(): String {
    KlassA({})
    KlassB()
    return "OK"
}

class KlassA(arg: (() -> Unit)?) {
    val func: (() -> Unit)? = arg
    init {
        if (func != null) {
            func()
        }
    }
}

class KlassB {
    val func: (() -> Unit)?
    init {
        if (true) {
            func = {}
            func()
        } else {
            func = null
        }
    }
}
