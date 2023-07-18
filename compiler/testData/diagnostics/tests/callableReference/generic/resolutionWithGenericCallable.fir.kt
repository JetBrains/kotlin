// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun test() {
    val a1: Array<Double.(Double) -> Double> = arrayOf(Double::plus, Double::minus)
    val a2: Array<Double.(Int) -> Double> = arrayOf(Double::plus, Double::minus)

    val a3: Array<Int.(Int) -> Double> = arrayOf(Double::<!UNRESOLVED_REFERENCE!>plus<!>, Double::<!UNRESOLVED_REFERENCE!>minus<!>)
    val a4: Array<Int.(Double) -> Double> = arrayOf(Int::plus, Double::<!UNRESOLVED_REFERENCE!>minus<!>)
    val a5: Array<Double.(Double) -> Double> = arrayOf(Double::plus, Int::<!UNRESOLVED_REFERENCE!>minus<!>)
}

fun foo(x: Int) {}
fun foo(y: String) {}

fun <T> bar(x: T, f: (T) -> Unit) {}

fun test2() {
    bar(1, ::foo)
    bar("", ::foo)
    bar(1.0, ::<!UNRESOLVED_REFERENCE!>foo<!>)
}
