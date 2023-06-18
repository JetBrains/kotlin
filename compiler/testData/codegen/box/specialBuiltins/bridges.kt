interface A0 {
    val size: Int get() = 56
}

class B0 : Collection<String>, A0 {
    override fun isEmpty() = throw UnsupportedOperationException()
    override fun contains(o: String) = throw UnsupportedOperationException()
    override fun iterator() = throw UnsupportedOperationException()
    override fun containsAll(c: Collection<String>) = throw UnsupportedOperationException()
    override val size: Int
        get() = super.size
}

open class A1 {
    val size: Int = 56
}

class B1 : Collection<String>, A1() {
    override fun isEmpty() = throw UnsupportedOperationException()
    override fun contains(o: String) = throw UnsupportedOperationException()
    override fun iterator() = throw UnsupportedOperationException()
    override fun containsAll(c: Collection<String>) = throw UnsupportedOperationException()
}

interface I2 {
    val size: Int
}

interface I3<T> {
    val size: T
}

interface I4<T> {
    val size: T get() = 56 as T
}

class B4 : Collection<String>, I4<Int> {
    override fun isEmpty() = throw UnsupportedOperationException()
    override fun contains(o: String) = throw UnsupportedOperationException()
    override fun iterator() = throw UnsupportedOperationException()
    override fun containsAll(c: Collection<String>) = throw UnsupportedOperationException()
    override val size: Int
        get() = super.size
}

interface I5 : Collection<String> {
    override val size: Int get() = 56
}

class B5 : I5 {
    override fun isEmpty() = throw UnsupportedOperationException()
    override fun contains(o: String) = throw UnsupportedOperationException()
    override fun iterator() = throw UnsupportedOperationException()
    override fun containsAll(c: Collection<String>) = throw UnsupportedOperationException()
}

fun box(): String {
    val b0 = B0()
    if (b0.size != 56) return "fail 0: ${b0.size}"
    var x: Collection<String> = B0()
    if (x.size != 56) return "fail 00: ${x.size}"
    val a0: A0 = b0
    if (a0.size != 56) return "fail 000: ${a0.size}"

    val b1 = B1()
    if (b1.size != 56) return "fail 1: ${b1.size}"
    x = B1()
    if (x.size != 56) return "fail 2: ${x.size}"

    val b4 = B4()
    if (b4.size != 56) return "fail 9: ${b4.size}"
    x = B4()
    if (x.size != 56) return "fail 10: ${x.size}"

    val b5 = B5()
    if (b5.size != 56) return "fail 11: ${b5.size}"
    x = B5()
    if (x.size != 56) return "fail 12: ${x.size}"

    return "OK"
}
