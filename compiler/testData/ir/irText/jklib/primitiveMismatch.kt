// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JavaSuper.java
public class JavaSuper {
    public void foo(double x) {}
}

// FILE: test.kt
class KotlinSub : JavaSuper() {
    fun foo(x: Char) {}
}
