// MODULE: m1-common
// FILE: common.kt

open class A {}
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}("B; B; some supertypes are missing in the actual declaration")!>expect class B : A<!>

expect open class A2() {}
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}("B2; B2; some supertypes are missing in the actual declaration")!>expect open class B2 : A2 {}<!>

expect open class A3

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias <!ACTUAL_WITHOUT_EXPECT("actual typealias B = A; The following declaration is incompatible because some supertypes are missing in the actual declaration:    expect class B : A")!>B<!> = A

actual typealias A2 = B2
actual open class <!ACTUAL_WITHOUT_EXPECT("actual class B2 : Any; The following declaration is incompatible because some supertypes are missing in the actual declaration:    expect class B2 : A2")!>B2<!> {}

actual typealias A3 = Any
