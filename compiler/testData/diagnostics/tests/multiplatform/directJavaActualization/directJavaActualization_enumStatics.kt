// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect enum class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Ok<!> {
    ENTRY;
    fun values() // Not to be confused with static values
}

expect enum class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>NoAnnotation<!> {
    ENTRY;
    fun values() // Not to be confused with static values
}

expect enum class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>ExcessiveAnnotation<!> {
    ENTRY;
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Ok.java
@kotlin.annotations.jvm.KotlinActual public enum Ok {
    ENTRY;
    @kotlin.annotations.jvm.KotlinActual public void values(){} // Not to be confused with static values
}

// FILE: NoAnnotation.java
@kotlin.annotations.jvm.KotlinActual public enum NoAnnotation {
    ENTRY;
    public void values(){} // Not to be confused with static values
}

// FILE: ExcessiveAnnotation.java
@kotlin.annotations.jvm.KotlinActual public enum ExcessiveAnnotation {
    ENTRY;
    @kotlin.annotations.jvm.KotlinActual public void values(){} // Not to be confused with static values
}
