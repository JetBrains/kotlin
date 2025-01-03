// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// ISSUE: KT-66723
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun <!AMBIGUOUS_ACTUALS{JVM}!>push<!>(value: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias Foo = FooImpl

// FILE: FooImpl.java
public class FooImpl {
    public void push(int value) {}
    public void push(Integer value) {}
}
