// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

interface B {
    override fun toString(): String
}

expect value <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class C<!>(val s: String) : B

expect value <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class D<!>(val s: String) : B

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@JvmInline
actual value class C(actual val s: String) : B {
    override fun <!ACTUAL_WITHOUT_EXPECT!>toString<!>(): String = s
}

@JvmInline
actual value class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>D<!>(actual val s: String) : B
