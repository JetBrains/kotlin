// WITH_STDLIB
// LANGUAGE: +FullValueClasses

value class PublicValue(val first: String, val second: Int)

value class PrivateValue(private val first: String, private val second: Int)
