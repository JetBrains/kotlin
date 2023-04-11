package test

import kotlin.reflect.KClass

class PropertyInitializer {
    annotation class Anno(
        val arrayWithDefault: Array<String> = ["a", "b", "c"],
        val enumWithDefault: E = E.B,
        val klassWithDefault: KClass<E> = E::class,
        //val klassWithDefaultDim: KClass<Array<*>> = Array::class,
        val annotationWithDefault: A1 = A1(E.A, E.B),
        val annotationArrayWithDefault: Array<A1> = [A1(E.A, E.B), A1(E.B, E.A)],
        val bool: Boolean = true,
        val byte: Byte = 1,
        val short: Short = 2,
        val int: Int = 3,
        val long: Long = 4,
        val float: Float = 5.0f,
        val dbl: Double = 6.0,
        val char: Char = '\n',
        val str: String = "str",
        val boolArray: BooleanArray
    )

    companion object {
        const val b: Byte = 100
        const val b1: Byte = 1
        const val s: Short = 20000
        const val s1: Short = 1
        const val i: Int = 2000000
        const val i1: Short = 1
        const val l: Long = 2000000000000L
        const val l1: Long = 1
        const val f: Float = 3.14f
        const val d: Double = 3.14
        const val bb: Boolean = true
        const val c: Char = '\u03c0' // pi symbol
        const val MAX_HIGH_SURROGATE: Char = '\uDBFF'
        const val nl = '\n'
        const val str: String = ":)"
    }
}
enum class E {
    A, B;
}

annotation class A1(val e: E, val e1: E)