// FIR_IDENTICAL
// LANGUAGE: +ReferencesToSyntheticJavaProperties
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: KotlinFile.kt

fun call(c: Any) {}

fun test() {
    JavaClass::foo
    call(JavaClass::foo)
}

// FILE: JavaClass.java

public class JavaClass {
    public String getFoo() {}
}
