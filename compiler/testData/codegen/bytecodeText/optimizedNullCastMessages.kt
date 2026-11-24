// LANGUAGE: +JvmOptimizedNullCastMessages
// TARGET_BACKEND: JVM

fun castToString(value: Any?): String = value as String
fun castToInt(value: Any?): Int = value as Int
fun castToArray(value: Any?): Array<String> = value as Array<String>
inline fun <reified T : Any> Any?.cast(): T = this as T
fun castToReifiedString(value: Any?): String = value.cast<String>()

// 4 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullInCast \(Ljava/lang/Object;Ljava/lang/Class;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
// 2 LDC Ljava/lang/String;.class
// 1 LDC Ljava/lang/Integer;.class
// 1 LDC \[Ljava/lang/String;.class
// 0 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 0 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getJavaObjectType
