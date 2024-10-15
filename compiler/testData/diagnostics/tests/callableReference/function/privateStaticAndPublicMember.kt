// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT
// FILE: A.java
class A {
    private static void foo() {}
    public String foo(int x) {}
}
// FILE: main.kt
fun main() {
    (A::foo)(A(), 1)
}
