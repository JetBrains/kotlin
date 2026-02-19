package pack

class OriginalClass

@JvmInline
value class ValueClass(val value: OriginalClass)

fun f<caret>oo(): ValueClass {

}
