// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect enum class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!> {
    ENTRY;
    fun getEntry()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public enum Foo {
    ENTRY("OK"){
        @Override
        public void getEntry() {
            super.getEntry();
        }
    };
    @kotlin.annotations.jvm.KotlinActual
    public void getEntry(){}
}