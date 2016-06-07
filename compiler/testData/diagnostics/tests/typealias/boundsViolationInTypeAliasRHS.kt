// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class TColl<T, C : Collection<T>>

typealias TCErr = TColl<String, <!UPPER_BOUND_VIOLATED!>Any<!>>
typealias TCErr2 = TCErr

fun testType1(x: TCErr) {}
val testCtor1 = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>TCErr()<!>

fun testType2(x: TCErr2) {}
val testCtor2 = <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>TCErr2()<!>
