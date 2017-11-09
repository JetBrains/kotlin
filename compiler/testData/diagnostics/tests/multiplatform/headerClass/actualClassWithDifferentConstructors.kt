// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class Foo1
expect class Foo2
expect class Foo3

expect class Bar1()
expect class Bar2()
expect class Bar3()
expect class Bar4()
expect class Bar5()

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

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar1<!>(val s: String)
actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar2<!>(val p: String = "value", <!UNUSED_PARAMETER!>i<!>: Int)
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar3<!> = JavaBar
actual class Bar4(val s: String) {
    <!ACTUAL_MISSING!>constructor() : this("")<!>
}

actual class Bar5 {
    actual constructor()
    constructor(s: String)
}