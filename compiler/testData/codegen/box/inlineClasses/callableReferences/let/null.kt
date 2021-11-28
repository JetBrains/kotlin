// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Value(val value: Any)

fun foo(value: Value?) = value?.value as String?

fun box(): String = (null as Value?).let(::foo) ?: "OK"
