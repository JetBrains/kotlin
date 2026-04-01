// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-20641
// LANGUAGE: +MultiPlatformProjects

// KT-20641: "Supertypes are missing" error when actual class hierarchy has additional intermediate class

// MODULE: common
// FILE: common.kt

open class Base

expect class Derived : Base

// MODULE: main()()(common)
// FILE: main.kt

open class Base2 : Base()

actual class Derived : Base2()

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect */
