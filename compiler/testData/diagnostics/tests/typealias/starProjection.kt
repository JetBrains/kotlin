// !WITH_NEW_INFERENCE
typealias A<T> = Map<T, T>
typealias B = A<*>

fun check(x: B) = x

fun test1(x: Map<Int, Int>) = check(x)

fun test2(x: Map<String, Int>) = check(x)

fun test3(x: Map<Int, String>) = check(x).size

fun test4(x: Map<Int, String>) = <!OI;MEMBER_PROJECTED_OUT!>check(x)[<!NI;TYPE_MISMATCH!>"42"<!>]<!>

fun test5(x: Map<Int, String>) = <!OI;MEMBER_PROJECTED_OUT!>check(x)[<!NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>]<!>