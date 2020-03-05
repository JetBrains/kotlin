// !LANGUAGE: +NewInference +ReferencesToSyntheticJavaProperties
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: KotlinFile.kt

fun call(c: Any) {}

fun test() {
    JavaClass::<!CALLABLE_REFERENCE_TO_JAVA_SYNTHETIC_PROPERTY!>foo<!>
    call(JavaClass::<!CALLABLE_REFERENCE_TO_JAVA_SYNTHETIC_PROPERTY!>foo<!>)
}

// FILE: JavaClass.java

public class JavaClass {
    public String getFoo() {}
}
