// RUN_PIPELINE_TILL: BACKEND
// Extracted from ReflectKotlinClass.kt

private val TYPES_ELIGIBLE_FOR_SIMPLE_VISIT = setOf<Class<*>>(
    // Primitives
    java.lang.Integer::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>, java.lang.Character::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>, java.lang.Byte::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>, java.lang.Long::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>,
    java.lang.Short::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>, java.lang.Boolean::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>, java.lang.Double::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>, java.lang.Float::class.<!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java<!>,
    // Arrays of primitives
    IntArray::class.java, CharArray::class.java, ByteArray::class.java, LongArray::class.java,
    ShortArray::class.java, BooleanArray::class.java, DoubleArray::class.java, FloatArray::class.java,
    // Others
    Class::class.java, String::class.java
)

/* GENERATED_FIR_TAGS: classReference, propertyDeclaration, starProjection */
