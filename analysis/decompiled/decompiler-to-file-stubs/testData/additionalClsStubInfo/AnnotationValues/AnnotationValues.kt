package test

import test.E.E1
import kotlin.reflect.KClass

const val CONSTANT = 12

class AnnotationValues {
    @Simple(
        12,
        12L,
        12,

        3.3,
        f = 3.3F,

        c = 'a',

        b1 = true,
        b2 = false
    )
    class WithSimple

    @StringLiteral("some", "", "H$CONSTANT")
    class WithStringLiteral

    @EnumLiteral(E1, E.E2, e3 = test.E.E2)
    class WithEnumLiteral

    @VarArg(1, 2, 3)
    class WithVarArg

    @Arrays(
        [1, 2, 3],
        [1L],
        [],
        [2.2],
        ['a'],
        [true, false]
    )
    class WithArrays

    @ClassLiteral(
        WithClassLiteral::class,
        String::class
    )
    class WithClassLiteral<T>

    @Outer("value", nested = Nested(12, "nested value"))
    class WithNested
}

annotation class Simple(
    val i: Int,
    val l: Long,
    val b: Byte,

    val d: Double,
    val f: Float,

    val c: Char,

    val b1: Boolean,
    val b2: Boolean
)

annotation class StringLiteral(
    val s1: String,
    val s2: String,
    val s3: String
)

enum class E {
    E1, E2
}
annotation class EnumLiteral(
    val e1: E,
    val e2: E,
    val e3: E
)

annotation class VarArg(
    vararg val v: Int
)

annotation class Arrays(
    val ia: IntArray,
    val la: LongArray,
    val fa: FloatArray,
    val da: DoubleArray,
    val ca: CharArray,
    val ba: BooleanArray
)

annotation class ClassLiteral(
    val c1: KClass<*>,
    val c2: KClass<*>
)


annotation class Nested(
    val i: Int,
    val s: String
)

annotation class Outer(
    val some: String,
    val nested: Nested
)
