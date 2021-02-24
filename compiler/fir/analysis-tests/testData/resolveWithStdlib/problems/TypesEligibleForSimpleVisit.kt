// Extracted from ReflectKotlinClass.kt

private val TYPES_ELIGIBLE_FOR_SIMPLE_VISIT = setOf<Class<*>>(
    // Primitives
    java.lang.Integer::class.java, java.lang.Character::class.java, java.lang.Byte::class.java, java.lang.Long::class.java,
    java.lang.Short::class.java, java.lang.Boolean::class.java, java.lang.Double::class.java, java.lang.Float::class.java,
    // Arrays of primitives
    IntArray::class.java, CharArray::class.java, ByteArray::class.java, LongArray::class.java,
    ShortArray::class.java, BooleanArray::class.java, DoubleArray::class.java, FloatArray::class.java,
    // Others
    Class::class.java, String::class.java
)
