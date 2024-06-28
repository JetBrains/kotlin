// TARGET_BACKEND: JVM_IR
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
// FILE: B_J.java
public class B_J extends A {
    public String foo() { return "O"; }
}

// FILE: C2_J.java
public class C2_J extends B_J {
    public String foo() { return "K"; }
}

// FILE: main.kt
actual class C1 : B_J()
actual typealias C2 = C2_J

fun box(): String {
    return commonBox()
}
