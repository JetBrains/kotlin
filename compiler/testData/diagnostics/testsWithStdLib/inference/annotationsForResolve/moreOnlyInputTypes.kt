// WITH_STDLIB

private val PRIMITIVE_CLASSES = listOf(Boolean::class, Byte::class, Char::class, Double::class, Float::class, Int::class, Long::class, Short::class)

// Map
private val PRIMITIVE_TO_WRAPPER = PRIMITIVE_CLASSES.map { it.javaPrimitiveType to it.javaObjectType }.toMap()
private val WRAPPER_TO_PRIMITIVE = PRIMITIVE_CLASSES.map { it.javaObjectType to it.javaPrimitiveType }.toMap()

val Class<*>.primitiveByWrapper: Class<*>?
    get() = WRAPPER_TO_PRIMITIVE[this]

val Class<*>.wrapperByPrimitive: Class<*>?
    get() = PRIMITIVE_TO_WRAPPER[this]
