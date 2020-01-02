// !DIAGNOSTICS: -UNUSED_PARAMETER

// KT-7174 Report error on members with the same signature as non-overridable methods from mapped Java types (like Object.wait/notify)

class A {
    fun notify() {}
    fun notifyAll() {}
    fun wait() {}
    fun wait(l: Long) {}
    fun wait(l: Long, i: Int) {}
    fun getClass(): Class<Any> = null!!
}

fun notify() {}
fun notifyAll() {}
fun wait() {}
fun wait(l: Long) {}
fun wait(l: Long, i: Int) {}
fun getClass(): Class<Any> = null!!
