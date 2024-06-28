// ISSUE: KT-56629, KT-56615

class Klass
fun foo(arg: Klass) {
    arg <!USELESS_CAST!>as Klass<!>
    arg <!USELESS_CAST!>as? Klass<!>
}

fun test_1(a: Any?) {
    (a as String?)!!
    a.length
    (a <!USELESS_CAST!>as? String<!>)!!
    a.length
}

fun test_3(a: Any?) {
    a as String
    a <!USELESS_CAST!>as String<!>
}

fun test_4(a: Any?) {
    a as String
    a <!USELESS_CAST!>as? String<!>
}

fun test_5(a: Any?) {
    (a as? String)!!
    a.length
    (a <!USELESS_CAST!>as String<!>)
}

fun test_6(a: Any?) {
    (a as? String)!!
    a.length
    (a <!USELESS_CAST!>as? String<!>)
}