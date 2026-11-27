// DISABLE_OPTIMIZATION

fun foo() {}

val bar = 1

fun functionReference(): Any = ::foo

fun propertyReference(): Any = ::bar

// 0 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 0 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaClass
