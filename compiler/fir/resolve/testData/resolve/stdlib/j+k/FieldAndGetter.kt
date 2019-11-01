// FILE: JavaClass.java

public class JavaClass {
    protected String field;

    public String getField() {
        return field;
    }
}

// FILE: test.kt

fun test(jc: JavaClass) {
    jc.<!AMBIGUITY!>field<!>
}