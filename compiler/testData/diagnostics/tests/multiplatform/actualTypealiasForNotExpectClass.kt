// MODULE: m1-common
// FILE: common.kt

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>A<!> {
    open fun foo(): String = "Fail"
}
expect class C1() : A
expect class C2() : A

// MODULE: m2-jvm()()(m1-common)
// FILE: A_J.java
public class A_J {}

// FILE: B_J.java
public class B_J extends A_J {
    public String foo() { return "O"; }
}

// FILE: C2_J.java
public class C2_J extends B_J {
    public String foo() { return "K"; }
}

// FILE: main.kt
actual typealias <!ACTUAL_WITHOUT_EXPECT, PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> = A_J

// Indirect subtyping is allowed in K2 KT-59356
actual class C1 : <!ACTUAL_WITHOUT_EXPECT!>B_J()<!>
actual typealias <!ACTUAL_WITHOUT_EXPECT!>C2<!> = C2_J
