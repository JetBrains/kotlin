// FILE: KotlinFile.kt

fun bar() = JavaClass::<!UNSUPPORTED!>foo<!>

// FILE: JavaClass.java

public class JavaClass {
    public String getFoo() {}
}
