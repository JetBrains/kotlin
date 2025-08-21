// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals

fun primitives(
        p01: Boolean,
        p02: Byte,
        p03: Char,
        p04: Double,
        p05: Float,
        p06: Int,
        p07: Long,
        p08: Short
) {}

fun nullablePrimitives(
        p01: Boolean?,
        p02: Byte?,
        p03: Char?,
        p04: Double?,
        p05: Float?,
        p06: Int?,
        p07: Long?,
        p08: Short?
) {}

fun primitiveArrays(
        p01: BooleanArray,
        p02: ByteArray,
        p03: CharArray,
        p04: DoubleArray,
        p05: FloatArray,
        p06: IntArray,
        p07: LongArray,
        p08: ShortArray
) {}

fun others(
        p01: Array<*>,
        p02: Array<String>,
        p03: Array<Array<Int?>?>,
        p04: List<*>,
        p05: List<String>?,
        p06: Map.Entry<Int, Double>,
        p07: Unit?,
        p08: String,
        p09: Nothing,
        p10: Array<Int>,
        p11: Array<Long>?,
) {}

inline fun <reified T : Any> wrapper(): KClass<T> = T::class

fun check(f: KFunction<*>, vararg expected: KClass<*>) {
    val actual = f.parameters.map { it.type.classifier as KClass<*> }
    for ((e, a) in expected.toList().zip(actual)) {
        assertEquals(e, a, "$e (${e.java}) != $a (${a.java})")
    }
}

fun box(): String {
    check(
            ::primitives,
            Boolean::class,
            Byte::class,
            Char::class,
            Double::class,
            Float::class,
            Int::class,
            Long::class,
            Short::class
    )

    check(
            ::nullablePrimitives,
            wrapper<Boolean>(),
            wrapper<Byte>(),
            wrapper<Char>(),
            wrapper<Double>(),
            wrapper<Float>(),
            wrapper<Int>(),
            wrapper<Long>(),
            wrapper<Short>()
    )

    check(
            ::primitiveArrays,
            BooleanArray::class,
            ByteArray::class,
            CharArray::class,
            DoubleArray::class,
            FloatArray::class,
            IntArray::class,
            LongArray::class,
            ShortArray::class
    )

    check(
            ::others,
            Array<Any>::class,
            Array<String>::class,
            Array<Array<Int?>?>::class,
            List::class,
            List::class,
            Map.Entry::class,
            Unit::class,
            String::class,
            Nothing::class,
            Array<Int>::class,
            Array<Long>::class,
    )

    return "OK"
}
