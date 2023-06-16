// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
//   IGNORE_REASON: new rules for supertypes matching are implemented only in K2
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-59356

// MODULE: common
open class A {
    open fun foo(): String = "Fail"
}
expect class C1() : A
expect class C2() : A

fun commonBox(): String {
    return C1().foo() + C2().foo()
}

// MODULE: platform-jvm()()(common)
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
actual typealias A = A_J
actual class C1 : B_J()
actual typealias C2 = C2_J

fun box(): String {
    return commonBox()
}
