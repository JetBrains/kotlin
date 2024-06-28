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
expect class Bar6()
expect class Bar7(s: String)

// MODULE: m2-jvm()()(m1-common)

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
actual class Foo2(val p: String = "value", i: Int)
actual typealias Foo3 = JavaFoo

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar1<!>(val s: String)
actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar2<!>(val p: String = "value", i: Int)
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Bar3<!> = JavaBar
actual class Bar4(val s: String) {
    <!ACTUAL_MISSING!>constructor() : this("")<!>
}

actual class Bar5 {
    actual constructor()
    constructor(s: String)
}

class <!ACTUAL_MISSING!>Bar6<!> {
    actual constructor()
}

actual class Bar7 actual constructor(s: String) {
    constructor() : this("")
}
