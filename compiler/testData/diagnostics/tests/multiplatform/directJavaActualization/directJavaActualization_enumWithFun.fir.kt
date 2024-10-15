// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect enum class Foo {
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