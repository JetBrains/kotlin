// !DIAGNOSTICS: -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER
open class Aaa() {
    val bar = 1
}

open class Bbb() : Aaa() {
    <!CONFLICTING_OVERLOADS!>val <T> bar<!> = "aa"
}