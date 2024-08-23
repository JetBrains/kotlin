// FIR_IDENTICAL
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect open class Foo {
    fun foo()
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Bar {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>open fun foo()<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual public class Foo {
    @kotlin.jvm.KotlinActual public final void foo() {}
}

// FILE: jvm.kt
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar<!> = Foo
