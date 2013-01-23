class MyRange1() : Range<Int> {
    override val start: Int
        get() = 0
    override val end: Int
        get() = 0
    override fun contains(item: Int) = true
}

class MyRange2() {
    fun contains(item: Int) = true
}

fun box(): String {
    if (1 in MyRange1()) {
        if (1 in MyRange2()) {
            return "OK"
        }
        return "fail 2"
    }
    return "fail 1"
}
