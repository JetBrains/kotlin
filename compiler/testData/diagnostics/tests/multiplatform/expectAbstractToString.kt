// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> {
    override fun toString(): String
}

expect value class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>(val s: String) : B

expect value class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>D<!>(val s: String) : B

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@JvmInline
actual value class C(actual val s: String) : B {
    override fun toString(): String = s
}

@JvmInline
actual value class D(actual val s: String) : B
