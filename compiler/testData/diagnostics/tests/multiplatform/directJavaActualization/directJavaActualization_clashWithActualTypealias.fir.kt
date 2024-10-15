// RUN_PIPELINE_TILL: FRONTEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo {}

// FILE: test.kt
actual typealias <!CLASSIFIER_REDECLARATION!>Foo<!> = Bar
class Bar
