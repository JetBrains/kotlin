// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
<!JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_ACTUAL_FUNCTION{JVM}!>expect class Foo : Base {
}<!>

open class Base {
    fun foo(a: Int = 1) {}
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo extends Base {
}
