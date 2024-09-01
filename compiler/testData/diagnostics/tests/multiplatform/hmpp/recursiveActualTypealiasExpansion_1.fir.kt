// ISSUE: KT-69632
// MODULE: common
expect class A
expect class B

expect fun commonFun(a: A)

// MODULE: intermediate1()()(common)
actual typealias A = B

// MODULE: intermediate2()()(intermediate1)
actual typealias B = <!RECURSIVE_TYPEALIAS_EXPANSION!>A<!>

// MODULE: main()()(intermediate2)
actual fun <!ACTUAL_WITHOUT_EXPECT!>commonFun<!>(a: A) {}

fun test() { <!UNRESOLVED_REFERENCE!>A<!>(); <!UNRESOLVED_REFERENCE!>B<!>(); }
