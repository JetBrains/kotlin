// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun test(param: String) {
    val a = ::<!UNSUPPORTED!>param<!>

    val local = "local"
    val b = ::<!UNSUPPORTED!>local<!>

    val lambda = { -> }
    val g = ::<!UNSUPPORTED!>lambda<!>
}
