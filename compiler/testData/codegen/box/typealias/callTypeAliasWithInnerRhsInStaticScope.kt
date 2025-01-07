// IGNORE_BACKEND_K2: ANY
// DUMP_IR
// ISSUE: KT-74107

class C {
    inner class Inner
    inner class Inner2<K>

    fun f() {
        TA()
        TA2<String>()
        val callable = ::TA
        callable()
    }
}

typealias TA = C.Inner
typealias TA2<K> = C.Inner2<K>

fun box(): String {
    val c = C()
    c.TA()
    c.TA2<String>()
    c.f()
    val callable = C::TA
    callable(c)

    with (c) {
        TA()
        TA2<Int>()
    }

    return "OK"
}