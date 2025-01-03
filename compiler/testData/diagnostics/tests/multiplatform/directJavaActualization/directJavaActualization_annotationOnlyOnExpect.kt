// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Annot<!>

@Annot
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo {}
