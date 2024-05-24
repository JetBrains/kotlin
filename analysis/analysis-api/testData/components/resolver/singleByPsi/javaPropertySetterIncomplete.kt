// FILE: call.kt
fun call() {
    val javaClass = JavaClass()
    // Intentionally incomplete to see if `foo` refers to `setFoo`.
    javaClass.<expr>foo</expr> =
}

// FILE: JavaClass.java
class JavaClass {
    private int foo = -1;
    int getFoo() { return foo; }
    void setFoo(int v) { foo = v; }
}
