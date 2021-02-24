// !WITH_NEW_INFERENCE
typealias A<T> = Map<T, T>
typealias B = A<*>

fun check(x: B) = x

fun test1(x: Map<Int, Int>) = check(x)

fun test2(x: Map<String, Int>) = check(x)

fun test3(x: Map<Int, String>) = check(x).size

fun test4(x: Map<Int, String>) = <!MEMBER_PROJECTED_OUT{OI}!>check(x)[<!TYPE_MISMATCH{NI}!>"42"<!>]<!>

fun test5(x: Map<Int, String>) = <!MEMBER_PROJECTED_OUT{OI}!>check(x)[<!CONSTANT_EXPECTED_TYPE_MISMATCH{NI}!>42<!>]<!>
