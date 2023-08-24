// FIR_IDENTICAL
// FILE: KotlinFile.kt
public interface I {
    public fun doIt()
}

fun foo(javaClass: JavaClass) {
    javaClass.<!DEPRECATION!>doSomething1<!> { bar() }
    javaClass.<!DEPRECATION!>doSomething2<!> { bar() }
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass {
    @Deprecated
    public void doSomething1(Runnable runnable) { runnable.run(); }

    /**
     * @deprecated
     */
    public void doSomething2(Runnable runnable) { runnable.run(); }
}