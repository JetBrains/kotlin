// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// LENIENT_MODE

// MODULE: common
// FILE: common.kt
expect enum class <!NO_ACTUAL_FOR_EXPECT{JVM}!>E<!> {
    Foo, Bar,
}

expect annotation class <!NO_ACTUAL_FOR_EXPECT{JVM}!>A<!>

expect value class <!NO_ACTUAL_FOR_EXPECT{JVM}!>V<!>(val s: String)

open class C1(s: String)

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>C2<!> : C1

// MODULE: jvm()()(common)
// FILE: jvm.kt
fun main() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, enumDeclaration, enumEntry, expect, functionDeclaration,
primaryConstructor, propertyDeclaration, value */
