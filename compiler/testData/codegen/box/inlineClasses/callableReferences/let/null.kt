inline class Value(val value: Any)

fun foo(value: Value?) = value?.value as String?

fun box(): String = (null as Value?).let(::foo) ?: "OK"
