// ISSUE: KT-44392, KT-56615

fun test_1(a: Any?) {
    (a as String?)!!
    (a <!USELESS_CAST!>as? String<!>)!!
}

fun test_2(a: Any?) {
    (a as String?)!!
    a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
}
