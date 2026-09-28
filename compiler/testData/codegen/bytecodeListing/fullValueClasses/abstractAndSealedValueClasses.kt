// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-89433

abstract class AbstractRegular

sealed class SealedRegular

abstract value class AbstractValue

sealed value class SealedValue

value class FinalValue(val x: Int, val y: Int)

value object ValueObject
