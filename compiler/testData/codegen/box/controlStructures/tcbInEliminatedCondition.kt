fun foo() {}

inline fun test1(v: Int) {
    if (v == 0) {
        try {
            foo()
        } catch (e: Exception) {
        }
    }
}

inline fun test2(v: Int) {
    try {
        if (v == 0) {
            foo()
        }
    } catch (e: Exception) {
    }
}

inline fun test3(v: Boolean) {
    if (v) {
        try {
            foo()
        } catch (e: Exception) {
        }
    }
}

inline fun test4(v: Boolean) {
    try {
        if (v) {
            foo()
        }
    } catch (e: Exception) {
    }
}

fun box(): String {
    test1(1)
    test2(1)
    test3(false)
    test4(false)
    return "OK"
}
