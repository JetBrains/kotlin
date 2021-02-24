// FILE: JavaClass.java

public class JavaClass {
    protected String field;

    public String getField() {
        return field;
    }
}

// FILE: test.kt

package some

fun test(jc: JavaClass) {
    jc.field
}