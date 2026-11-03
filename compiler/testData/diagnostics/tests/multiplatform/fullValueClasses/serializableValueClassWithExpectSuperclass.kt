// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21

// MODULE: m1-common
// FILE: common.kt
import java.io.Serializable

expect abstract value class Base() : Serializable

value class Sub(val x: Int) : Base()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
import java.io.Serializable

actual abstract value class Base actual constructor() : Serializable {
    protected fun writeReplace(): Any = 0
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, value */
