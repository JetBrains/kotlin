package test

import kotlin.annotation.AnnotationTarget.*

@Retention(AnnotationRetention.BINARY)
@Target(CLASS, CONSTRUCTOR, FUNCTION, PROPERTY, VALUE_PARAMETER, TYPE, TYPE_PARAMETER)
annotation class A

@A
class Klass @A constructor()

@A
fun <@A T> function(@A param: Unit): @A Unit {}

@A
val property = Unit

enum class Enum {
    @A
    ENTRY
}
