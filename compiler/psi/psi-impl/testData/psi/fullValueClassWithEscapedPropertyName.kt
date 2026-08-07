// WITH_STDLIB
// LANGUAGE: +FullValueClasses

value class PublicValue(val `underlying property`: String)

value class PrivateValue(private val `underlying property`: String)
