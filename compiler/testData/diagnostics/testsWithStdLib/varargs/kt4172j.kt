// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java
public class A {
    public static void main(String[] args) {}
}

// FILE: 1.kt
fun main() {
    A.main(arrayOf())
}