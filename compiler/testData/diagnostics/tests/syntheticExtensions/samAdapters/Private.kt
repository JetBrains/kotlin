// !WITH_NEW_INFERENCE
// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.<!INVISIBLE_MEMBER!>doSomething<!> <!TYPE_MISMATCH!>{ }<!>
}

// FILE: JavaClass.java
public class JavaClass {
    private void doSomething(Runnable runnable) { runnable.run(); }
}