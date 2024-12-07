// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
open class InjectedEmptySuperClass()
expect class Foo {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo extends InjectedEmptySuperClass {
    @kotlin.annotations.jvm.KotlinActual
    public void foo(){}
}
