// LANGUAGE: +JvmOptimizedNullCastMessages
// TARGET_BACKEND: JVM

fun castToString(value: Any?): String = value as String
fun castToInt(value: Any?): Int = value as Int
fun castToArray(value: Any?): Array<String> = value as Array<String>
inline fun <reified T : Any> Any?.cast(): T = this as T
fun castToReifiedString(value: Any?): String = value.cast<String>()

// 4 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullInCast \(Ljava/lang/Object;Ljava/lang/Class;\)V
// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
