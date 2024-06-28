// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE

// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<String>): String {
    return javaClass.doSomething("", 1) { s: String -> "" }
}

// FILE: JavaClass.java
public class JavaClass<T> {
    public T doSomething(T t, int anInt, I<T> i) { return t; }
}

// FILE: I.java
interface I<T> {
    T doIt(T t);
}
