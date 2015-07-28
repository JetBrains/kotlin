// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.isSomething = !javaClass.isSomething
    javaClass.isSomething2 = !javaClass.isSomething2

    javaClass.<!UNRESOLVED_REFERENCE!>something<!>
    javaClass.<!FUNCTION_CALL_EXPECTED!>isSomethingWrong<!>
    javaClass.<!UNRESOLVED_REFERENCE!>somethingWrong<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public boolean isSomething() {
        return true;
    }

    public void setSomething(boolean value) {
    }

    public boolean getIsSomething2() {
        return true;
    }

    public void setIsSomething2(boolean value) {
    }

    public int isSomethingWrong() {
        return 1;
    }
}