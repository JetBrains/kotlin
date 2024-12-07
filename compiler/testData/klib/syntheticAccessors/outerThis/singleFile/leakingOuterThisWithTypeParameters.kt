// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// WITH_STDLIB

class Outer<O>(private val o: O) {
    fun getO(): O = o

    inner class InnerL1<I1>(private val i1: I1) {
        fun getI1(): I1 = i1

        inner class InnerL2<I2>(private val i2: I2) {
            fun getI2(): I2 = i2

            inline fun getAll(): Triple<O, I1, I2> = Triple(getO(), getI1(), getI2())
        }
    }
}

fun box(): String {
    val result = Outer(42).InnerL1("hello").InnerL2(listOf(3.14)).getAll().toString()
    return if (result == "(42, hello, [3.14])") "OK" else result
}
