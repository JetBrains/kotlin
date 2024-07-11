package pack

class OriginalClass

@JvmInline
value class AnotherValueClass(val original: OriginalClass)

@JvmInline
value class ValueClass(val value: AnotherValueClass)

fun f<caret>oo() = null as? ValueClass
