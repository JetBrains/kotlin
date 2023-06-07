// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun eat(value: Any) {}

fun test(param: String) {
    val a = ::<!UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS!>param<!>

    val local = "local"
    val b = ::<!UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS!>local<!>

    val lambda = { -> }
    val g = ::<!UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS!>lambda<!>

    eat(::<!UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS!>param<!>)
}
