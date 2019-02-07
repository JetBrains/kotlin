class C() {
    fun foo(): Int {
        fun local(i: Int = 1): Int {
            return i
        }
        return local()
    }
}

fun box(): String {
    fun local(i: Int = 1): Int {
        return i
    }

    if (local() != 1) return "Fail 1"
    if (local(2) != 2) return "Fail 2"
    if (C().foo() != 1) return "Fail 3"

    return "OK"
}
