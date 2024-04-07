// MODULE: m1-common
// FILE: common.kt

open class A {}
expect class B : A

expect open class A2() {}
expect open class B2 : A2 {}

expect open class A3

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!ACTUAL_WITHOUT_EXPECT!>B<!> = A

actual typealias A2 = B2
actual open class <!ACTUAL_WITHOUT_EXPECT!>B2<!> {}

actual typealias A3 = Any
