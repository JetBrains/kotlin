// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.something = !javaClass.something

    javaClass.<!UNRESOLVED_REFERENCE!>somethingWrong<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public boolean isSomething() { return true; }
    public void setSomething(boolean value) { }
    public int isSomethingWrong() { return 1; }
}