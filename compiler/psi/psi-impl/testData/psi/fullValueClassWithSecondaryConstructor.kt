// WITH_STDLIB
// LANGUAGE: +FullValueClasses

value class PublicValue(val value: Int) {
    constructor(value: Long) : this(value.toInt())
}
