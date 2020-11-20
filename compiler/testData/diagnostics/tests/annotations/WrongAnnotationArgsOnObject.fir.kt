package test

<!INAPPLICABLE_CANDIDATE!>@BadAnnotation(1)<!>
object SomeObject

val some = SomeObject

annotation class BadAnnotation(val s: String)
