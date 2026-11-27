// LANGUAGE: +JvmOptimizedNullCastMessages +JvmOptimizedNullCastMessagesInInlineFunctions
// TARGET_BACKEND: JVM

inline fun castInInlineScope(value: Any?): String = value as String
inline fun <reified T : Any> Any?.cast(): T = this as T
inline fun castReifiedInInlineScope(value: Any?): String = value.cast<String>()

// 0 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNull \(Ljava/lang/Object;Ljava/lang/String;\)V
// 2 INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullInCast \(Ljava/lang/Object;Ljava/lang/Class;\)V
