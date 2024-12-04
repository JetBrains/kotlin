// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt

annotation class Annot

@Annot
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT{JVM}, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>expect<!> class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo {}
