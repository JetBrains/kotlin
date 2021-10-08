// WITH_REFLECT
// TARGET_BACKEND: JVM
// WITH_RUNTIME

annotation class Ann(val value: String)

@JvmInline
value class C<T>(val x: String)

@Ann("OK")
val <T> C<T>.value: String
    get() = x

fun box() = (C<Any?>::value.annotations.singleOrNull() as? Ann)?.value ?: "null"
