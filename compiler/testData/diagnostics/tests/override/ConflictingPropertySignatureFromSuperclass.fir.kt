// DIAGNOSTICS: -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER
open class Aaa() {
    val bar = 1
}

open class Bbb() : Aaa() {
    val <T> bar = "aa"
}