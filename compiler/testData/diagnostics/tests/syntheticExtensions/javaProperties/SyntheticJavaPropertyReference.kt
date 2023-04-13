// !LANGUAGE: -ReferencesToSyntheticJavaProperties
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// FIR_IDENTICAL

// FILE: KotlinFile.kt

fun call(c: Any) {}

fun test() {
    JavaClass::<!UNSUPPORTED_FEATURE!>foo<!>
    call(JavaClass::<!UNSUPPORTED_FEATURE!>foo<!>)
}

// FILE: JavaClass.java

public class JavaClass {
    public String getFoo() {}
}
