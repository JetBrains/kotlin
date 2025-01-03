// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>InjectedEmptySuperClass<!>()
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!> {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo extends InjectedEmptySuperClass {
    @kotlin.annotations.jvm.KotlinActual
    public void foo(){}
}
