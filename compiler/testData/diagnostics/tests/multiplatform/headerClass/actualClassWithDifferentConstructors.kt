// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2
expect class Foo3

expect class Bar1()
expect class Bar2()
expect class Bar3()

// MODULE: m2-jvm(m1-common)

// FILE: JavaFoo.java

public class JavaFoo {
    public JavaFoo(int i) {}
}

// FILE: JavaBar.java

public class JavaBar {
    public JavaBar(int i) {}
}

// FILE: jvm.kt

actual class Foo1(val s: String)
actual class Foo2(val p: String = "value", <!UNUSED_PARAMETER!>i<!>: Int)
actual typealias Foo3 = JavaFoo

actual class Bar1<!ACTUAL_WITHOUT_EXPECT!>(val s: String)<!>
actual class Bar2<!ACTUAL_WITHOUT_EXPECT!>(val p: String = "value", <!UNUSED_PARAMETER!>i<!>: Int)<!>
<!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>actual typealias Bar3 = JavaBar<!>