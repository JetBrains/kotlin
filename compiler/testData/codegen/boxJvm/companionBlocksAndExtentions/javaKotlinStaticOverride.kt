// LANGUAGE: +CompanionBlocksAndExtensions
// TARGET_BACKEND: JVM
// ISSUE: KT-86109
// FILE: A.java
public class A {
    static String foo() {
        return "O";
    }
}
// FILE: B.kt
class B : A() {
    companion {
        fun foo() = "K"
    }
}
// FILE: main.kt
fun box(): String {
    return A.foo() + B.foo()
}
