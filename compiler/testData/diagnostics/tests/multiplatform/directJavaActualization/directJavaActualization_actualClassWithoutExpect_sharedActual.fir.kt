// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
<!JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT{JVM}!>expect<!> class Foo

expect class Bar {
    class TypealiasOnly
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo {
    @kotlin.annotations.jvm.KotlinActual public static class TypealiasOnly {}
}

// FILE: jvm.kt
actual typealias Bar = Foo
