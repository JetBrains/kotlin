// RUN_PIPELINE_TILL: FRONTEND
// SKIP_FIR_DUMP
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

// FILE: expect.kt

expect interface I {
    <!WRONG_MODIFIER_TARGET!>expect<!> typealias A<!SYNTAX!><!>
<!SYNTAX!><!>}

// FILE: expect1.kt

expect class E1 {
    class I
}

// FILE: expect2.kt

expect class E2 {
    class I
}

// FILE: expect3.kt

expect class E3 {
    class I
}

// FILE: expect4.kt

open class IBase {
    typealias A = String
    open fun foo(a: A) {}
}

expect class Base : IBase {
    override fun foo(a: IBase.A)
}

// MODULE: platform()()(common)

// FILE: actual.kt

actual interface <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>I<!> {
    typealias A = String
}

// FILE: actual1.kt

actual class E1 {
    actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_NESTED_TYPE_ALIAS!>I<!> = Int
}

// FILE: actual2.kt

class A {
    typealias I = Int
}

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>E2<!> = A

// FILE: actual3.kt

class B {
    class I
}

actual typealias E3 = B  // OK

// FILE: actual4.kt

actual class Base: IBase() {
    actual override fun foo(a: String) { }
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, interfaceDeclaration, nestedClass,
override, typeAliasDeclaration */
