inline fun <R> doCall(p: () -> R) {
    p()
}

inline fun <R> doCallInt(p: () -> R): R {
    return p()
}

class A {
    var result: Int = doCallInt { return this };

    var field: Int
        get() {
            doCall { return 1 }
            return 2
        }
        set(v: Int) {
            doCall {
                result = v / 2
                return
            }
            result = v
        }
}