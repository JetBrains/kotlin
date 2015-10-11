open class A : CharSequence {
    override val length: Int = 123

    override fun get(index: Int) = 'z';

    override fun subSequence(start: Int, end: Int): CharSequence {
        throw UnsupportedOperationException()
    }
}

fun box(): String {
    val b = J.B()
    val a = A()

    if (b[0] != 'z') return "fail 6"
    if (a[0] != 'z') return "fail 7"
    if (b[1] != 'a') return "fail 8"
    if (a[0] != 'z') return "fail 9"

    if (b.get(0) != 'z') return "fail 10"
    if (a.get(0) != 'z') return "fail 11"
    if (b.get(1) != 'a') return "fail 12"
    if (a.get(1) != 'z') return "fail 13"

    var cs: CharSequence = a
    if (a.length != 123) return "fail 14"
    if (cs.length != 123) return "fail 15"

    cs = b
    if (b.length != 456) return "fail 16"
    if (b.length != 456) return "fail 17"

    return J.foo();
}
