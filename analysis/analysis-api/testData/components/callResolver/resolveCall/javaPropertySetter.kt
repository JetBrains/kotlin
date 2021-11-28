// FILE: call.kt
fun call() {
    val javaClass = JavaClass()
    javaClass.<expr>foo</expr> = 42
}

// FILE: JavaClass.java
class JavaClass {
    private int foo = -1;
    int getFoo() { return foo; }
    void setFoo(int v) { foo = v; }
}
