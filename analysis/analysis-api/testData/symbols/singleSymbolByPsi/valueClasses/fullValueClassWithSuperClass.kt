// LANGUAGE: +FullValueClasses

sealed value class SealedValue(parameter: Int)

value class FullValueClassWithSuper<caret>Class(val value: String) : SealedValue(0)
