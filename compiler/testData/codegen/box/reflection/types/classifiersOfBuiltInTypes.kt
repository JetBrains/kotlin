// IGNORE_BACKEND_FIR: JVM_IR
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
        p1: Array<*>,
        p2: Array<String>,
        p3: Array<Array<Int?>?>,
        p4: List<*>,
        p5: List<String>?,
        p6: Map.Entry<Int, Double>,
        p7: Unit?,
        p8: String,
        p9: Nothing
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
            Nothing::class
    )

    return "OK"
}
