// WITH_KOTLIN_JVM_ANNOTATIONS
// MODULE: m1-common
// FILE: common.kt

expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

@kotlin.annotations.jvm.KotlinActual public class Foo {
    @kotlin.annotations.jvm.KotlinActual public Foo() {}
    @kotlin.annotations.jvm.KotlinActual public void foo() {
    }
}
