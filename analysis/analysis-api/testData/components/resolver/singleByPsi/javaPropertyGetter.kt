// FILE: call.kt
fun call() {
    val javaClass = JavaClass()
    javaClass.<expr>foo</expr>
}

// FILE: JavaClass.java
class JavaClass {
    int getFoo() { return 42; }
}

// IGNORE_STABILITY: candidates
// ^KT-69962
