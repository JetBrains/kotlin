// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

internal sealed interface ValueType {
    OPTIONAL_JVM_INLINE_ANNOTATION
    value class Number(val numberType: NumberType) : ValueType
    OPTIONAL_JVM_INLINE_ANNOTATION
    value class Other(val subtype: Subtype) : ValueType
}

internal enum class NumberType { I32 }

internal interface Subtype {
    val id: Int
    OPTIONAL_JVM_INLINE_ANNOTATION
    value class Ref(override val id: Int) : Subtype
}

fun foo(f: Boolean): Int {
    val otherTypes: List<ValueType.Other> = listOf(ValueType.Other(Subtype.Ref(42)))
    val id = otherTypes.first().subtype.id

    val numberTypeProvider: () -> NumberType = { NumberType.I32 }
    val otherTypeProvider: () -> Subtype = { Subtype.Ref(1) }

    if (f)
        ValueType.Number(numberTypeProvider())
    else
        ValueType.Other(otherTypeProvider())

    return id
}

fun box(): String {
    val x = foo(false)
    if (x != 42) return "fail: $x"
    return "OK"
}