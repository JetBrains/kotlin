package test

@BadAnnotation(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
object SomeObject

val some = SomeObject

annotation class BadAnnotation(val s: String)
