// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E<!> {
    FOO, BAR
}

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Matching<!>(val e: E = E.FOO)

expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>NonMatching<!>(val e: E = E.BAR)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias E = EJava
actual typealias Matching = AJava
actual typealias <!ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE!>NonMatching<!> = AJava

// FILE: EJava.java
public enum EJava {
    FOO, BAR;

    EJava() {}
}

// FILE: AJava.java
public @interface AJava {
    EJava e() default EJava.FOO;
}
