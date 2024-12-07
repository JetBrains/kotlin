// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
open class Base() {
    open fun fakeOverride() {}
}

expect class Foo : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo extends Base {
    @kotlin.annotations.jvm.KotlinActual public Foo() {}
    @kotlin.annotations.jvm.KotlinActual public void foo() {}
    @kotlin.annotations.jvm.KotlinActual @Override void fakeOverride() {}
}
