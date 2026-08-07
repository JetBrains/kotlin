// WITH_STDLIB
// LANGUAGE: +FullValueClasses

sealed value class SealedValue(parameter: Int)

value class DerivedValue(val value: String) : SealedValue(0)
