// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J {
    public static void primitives(
        boolean p01, byte p02, char p03, double p04,
        float p05, int p06, long p07, short p08
    ) {}
    
    public static void wrappers(
        Boolean p01, Byte p02, Character p03, Double p04,
        Float p05, Integer p06, Long p07, Short p08
    ) {}
    
    public static void primitiveArrays(
        boolean[] p01, byte[] p02, char[] p03, double[] p04,
        float[] p05, int[] p06, long[] p07, short[] p08
    ) {}

    public static void wrapperArrays(
        Boolean[] p01, Byte[] p02, Character[] p03, Double[] p04,
        Float[] p05, Integer[] p06, Long[] p07, Short[] p08
    ) {}
}

// FILE: test.kt
import kotlin.reflect.*
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

fun wrappers(
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

fun wrapperArrays(
        p01: Array<Boolean>,
        p02: Array<Byte>,
        p03: Array<Char>,
        p04: Array<Double>,
        p05: Array<Float>,
        p06: Array<Int>,
        p07: Array<Long>,
        p08: Array<Short>
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

fun checkClassifiers(f: KFunction<*>, vararg expected: KClass<*>) {
    val actual = f.parameters.map { it.type.classifier as KClass<*> }
    for ([e, a] in expected.toList().zip(actual)) {
        assertEquals(e, a, "$e (${e.java}) != $a (${a.java})")
    }
}

fun checkArguments(f: KFunction<*>) {
    val expected = List(f.parameters.size) { emptyList<KTypeProjection>() }
    val actual = f.parameters.map { it.type.arguments }
    assertEquals(expected, actual, "Non-empty arguments for some types in $f")
}

fun box(): String {
    for (f in listOf(::primitives, J::primitives)) {
        checkClassifiers(
                f,
                Boolean::class,
                Byte::class,
                Char::class,
                Double::class,
                Float::class,
                Int::class,
                Long::class,
                Short::class
        )
        checkArguments(f)
    }

    for (f in listOf(::wrappers, J::wrappers)) {
        checkClassifiers(
                f,
                wrapper<Boolean>(),
                wrapper<Byte>(),
                wrapper<Char>(),
                wrapper<Double>(),
                wrapper<Float>(),
                wrapper<Int>(),
                wrapper<Long>(),
                wrapper<Short>()
        )
        checkArguments(f)
    }

    for (f in listOf(::primitiveArrays, J::primitiveArrays)) {
        checkClassifiers(
                f,
                BooleanArray::class,
                ByteArray::class,
                CharArray::class,
                DoubleArray::class,
                FloatArray::class,
                IntArray::class,
                LongArray::class,
                ShortArray::class
        )
        checkArguments(f)
    }

    for (f in listOf(::wrapperArrays, J::wrapperArrays)) {
        checkClassifiers(
                f,
                Array<Boolean>::class,
                Array<Byte>::class,
                Array<Char>::class,
                Array<Double>::class,
                Array<Float>::class,
                Array<Int>::class,
                Array<Long>::class,
                Array<Short>::class
        )
    }

    checkClassifiers(
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
