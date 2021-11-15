// WITH_REFLECT
// TARGET_BACKEND: JVM
// WITH_STDLIB

annotation class Ann(val value: String)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class C<T>(val x: String)

@Ann("OK")
val <T> C<T>.value: String
    get() = x

fun box() = (C<Any?>::value.annotations.singleOrNull() as? Ann)?.value ?: "null"
