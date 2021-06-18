// JAVAC_EXPECTED_FILE
// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass): String {
    return javaClass.doSomething("") <!ARGUMENT_TYPE_MISMATCH!>{ <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>it<!> }<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public <T> T doSomething(T t, I<T> i) { return i.run(t); }
}

interface I<T> {
    T run(T t);
}
