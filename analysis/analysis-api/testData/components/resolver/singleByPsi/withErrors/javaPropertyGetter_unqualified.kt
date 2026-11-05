// FILE: call.kt
fun JavaClass.call() {
    <expr>foo</expr>
}

// FILE: JavaClass.java
class JavaClass {
    int getFoo() { return 42; }
}
