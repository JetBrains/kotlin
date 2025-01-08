// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-69724
// MODULE: common
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class A<!>
expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{JVM}!>class B<!>

expect fun commonFun<!NO_ACTUAL_FOR_EXPECT{JVM}!>(a: <!RECURSIVE_TYPEALIAS_EXPANSION{JVM}!>A<!>)<!>

// MODULE: intermediate1()()(common)
actual typealias <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>A<!> = <!RECURSIVE_TYPEALIAS_EXPANSION{JVM}!>B<!>

// MODULE: intermediate2()()(common)
actual typealias <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>B<!> = <!RECURSIVE_TYPEALIAS_EXPANSION{JVM}!>A<!>

// MODULE: main()()(intermediate1, intermediate2)
actual fun commonFun<!ACTUAL_WITHOUT_EXPECT!>(a: <!RECURSIVE_TYPEALIAS_EXPANSION!>A<!>)<!> {}

fun test() {
    <!UNRESOLVED_REFERENCE!>A<!>()
    <!UNRESOLVED_REFERENCE!>B<!>()
}
