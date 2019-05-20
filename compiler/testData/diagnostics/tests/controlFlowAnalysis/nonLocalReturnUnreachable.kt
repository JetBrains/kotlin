// !WITH_NEW_INFERENCE
// See also KT-5198 / KT-10186

inline fun doCall(f: () -> Unit) = f()

fun test1(nonLocal: String): String {
    val localResult = doCall {
        return nonLocal  //unreachable
    }
    return "NON_LOCAL_FAILED $localResult"  //unreachable
}

fun doSomething() {}

fun test2() {
    fun f(x: Any?) = x
    f(null?.let { return })

    // false unreachable here
    doSomething()
}

fun test3(x: Any?): Boolean =
    x?.let {
        return true
    } ?: false
