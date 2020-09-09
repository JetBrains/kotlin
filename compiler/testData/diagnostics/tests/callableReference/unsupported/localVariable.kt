// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun eat(value: Any) {}

fun test(param: String) {
    val a = ::<!UNSUPPORTED!>param<!>

    val local = "local"
    val b = ::<!UNSUPPORTED!>local<!>

    val lambda = { -> }
    val g = ::<!UNSUPPORTED!>lambda<!>

    eat(::<!UNSUPPORTED!>param<!>)
}
