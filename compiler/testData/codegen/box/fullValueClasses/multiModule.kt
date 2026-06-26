// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// DUMP_IR
// DUMP_KLIB_ABI: DEFAULT

// MODULE: lib
// FILE: lib.kt

value class SingleFieldValueClass private constructor(private val x: Int) {
    fun retrieveX() = x
    companion object {
        fun make() = SingleFieldValueClass(1)
    }
}

value class MultiFieldValueClass private constructor(private val x: Int, private val y: Int) {
    fun retrieveX() = x
    fun retrieveY() = y
    companion object {
        fun make() = MultiFieldValueClass(1, 2)
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val sfvc = SingleFieldValueClass.make()
    sfvc.retrieveX().let { if (it != 1) error("X is not 1") }
    val mfvc = MultiFieldValueClass.make()
    mfvc.retrieveX().let { if (it != 1) error("X is not 1") }
    mfvc.retrieveY().let { if (it != 2) error("Y is not 2") }
    return "OK"
}
