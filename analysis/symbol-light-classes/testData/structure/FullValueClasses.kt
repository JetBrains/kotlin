// LANGUAGE: +FullValueClasses
package cba

abstract value class AbstractValueClass

sealed value class SealedValueClass : AbstractValueClass()

value class FinalValueClass(val first: Int, val second: Int) : SealedValueClass()

value object ValueObject : SealedValueClass()

class RegularClass : SealedValueClass()

value class StandaloneValueClass(val first: Int, val second: Int)
