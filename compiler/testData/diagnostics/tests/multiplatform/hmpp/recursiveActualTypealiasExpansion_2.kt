// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-69724
// MODULE: common
expect class <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{JVM}!>A<!>
expect class <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{JVM}!>B<!>

expect fun <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}!>commonFun<!><!NO_ACTUAL_FOR_EXPECT{JVM}!>(a: <!RECURSIVE_TYPEALIAS_EXPANSION{JVM}!>A<!>)<!>

// MODULE: intermediate1()()(common)
actual typealias A = <!RECURSIVE_TYPEALIAS_EXPANSION{JVM}!>B<!>

// MODULE: intermediate2()()(common)
actual typealias B = <!RECURSIVE_TYPEALIAS_EXPANSION{JVM}!>A<!>

// MODULE: main()()(intermediate1, intermediate2)
actual fun commonFun<!ACTUAL_WITHOUT_EXPECT!>(a: <!RECURSIVE_TYPEALIAS_EXPANSION!>A<!>)<!> {}

fun test() {
    <!UNRESOLVED_REFERENCE!>A<!>()
    <!UNRESOLVED_REFERENCE!>B<!>()
}
