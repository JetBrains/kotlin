// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT{JVM}!>expect<!> fun interface Foo {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public interface Foo extends Base {
    @kotlin.annotations.jvm.KotlinActual public void foo();
}

// FILE: Base.java
public interface Base {
    void bar();
}
