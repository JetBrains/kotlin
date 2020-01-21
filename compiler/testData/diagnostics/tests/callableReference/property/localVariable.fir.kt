// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun eat(value: Any) {}

fun test(param: String) {
    val a = <!UNRESOLVED_REFERENCE!>::param<!>

    val local = "local"
    val b = ::local

    val lambda = { -> }
    val g = ::lambda

    eat(::param)
}
