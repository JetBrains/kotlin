// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: NATIVE

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

val list = ArrayList<String>()

class B2 : ArrayList<String>(list), I2

interface I3<T> {
    val size: T
}

class B3 : ArrayList<String>(list), I3<Int>

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
    list.add("1")

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

    val b2 = B2()
    if (b2.size != 1) return "fail 3: ${b2.size}"
    x = B2()
    if (x.size != 1) return "fail 4: ${x.size}"
    val i2: I2 = b2
    if (i2.size != 1) return "fail 5: ${i2.size}"

    val b3 = B3()
    if (b3.size != 1) return "fail 6: ${b3.size}"
    x = B3()
    if (x.size != 1) return "fail 7: ${x.size}"
    val i3: I3<Int> = b3
    if (i3.size != 1) return "fail 8: ${i3.size}"

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
