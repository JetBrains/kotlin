class A(var n: Int) {
    fun plusAssign(m: Int) {
        n += m
    }
}

fun test() {
    val a = A(0)
    a.plusAssign(1)
    a += 1
}