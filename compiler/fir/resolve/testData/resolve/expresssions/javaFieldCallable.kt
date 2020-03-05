// FILE: JavaClass.java

public class JavaClass {
    public static String staticField;
    public String nonStaticField;
}

// FILE: test.kt

fun test() {
    val staticReference = JavaClass::staticField
    val nonStaticReference = JavaClass::nonStaticField
}