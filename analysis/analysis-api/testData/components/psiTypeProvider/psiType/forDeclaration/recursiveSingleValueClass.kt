package pack

@JvmInline
value class ValueClass(val value: ValueClass)

fun f<caret>oo(): ValueClass {}
