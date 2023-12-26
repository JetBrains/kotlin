// JVM_ABI_K1_K2_DIFF: KT-62464

inline fun test(crossinline l: () -> String) {
    {
        l()
    }.let { it() }

    object {
        val z = l() //constuctor
    }
}


fun box(): String {
    var z = "fail"
    test {
        synchronized("123") {
            z = "OK"
            z
        }
    }

    return z
}

// 0 finallyStart
// 0 finallyEnd