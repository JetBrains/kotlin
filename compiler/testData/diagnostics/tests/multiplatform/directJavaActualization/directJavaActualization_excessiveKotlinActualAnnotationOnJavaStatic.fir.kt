// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization

// MODULE: m1-common
// FILE: common.kt
<!JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT{JVM}!>expect<!> class Foo {
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo {
    @kotlin.annotations.jvm.KotlinActual public static void foo() { }
}
