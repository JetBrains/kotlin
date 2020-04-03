// !LANGUAGE: -ReferencesToSyntheticJavaProperties
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: KotlinFile.kt

fun call(c: Any) {}

fun test() {
    JavaClass::<!UNSUPPORTED!>foo<!>
    call(JavaClass::<!UNSUPPORTED!>foo<!>)
}

// FILE: JavaClass.java

public class JavaClass {
    public String getFoo() {}
}
