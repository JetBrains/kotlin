// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val s: String)

expect value class <!NO_ACTUAL_FOR_EXPECT!>B<!>(val s: String)

expect value class <!NO_ACTUAL_FOR_EXPECT!>C<!>(val s: String)

expect value class <!NO_ACTUAL_FOR_EXPECT!>D<!>(val s: String)

expect value class <!NO_ACTUAL_FOR_EXPECT!>E<!>(val s: String)

interface I {
    fun box()
    fun unbox()
}

// MODULE: jvm()()(common)
// FILE: J.java
public interface J {
    void box();
    void unbox();
}
// FILE: jvm.kt
@JvmInline
actual value class A(val s: String) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}
}

@JvmInline
actual value class B(val s: String) : J {
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}
}

@JvmInline
actual value class C(val s: String) : I {
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}
}

@JvmInline
actual value class D(val s: String) {
    <!INAPPLICABLE_JVM_NAME!>@JvmName("box")<!>
    fun foo() {}
    <!INAPPLICABLE_JVM_NAME!>@JvmName("unbox")<!>
    fun bar() {}
}
