// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I<!> {
    fun f(x: Int = 5) = x
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E<!> : I {
    override fun f(x: Int): Int
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E2<!> : I {
    override fun f(x: Int): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class E(i: I) : I by i

actual class E2(i: I) : I by i {
    actual override fun f(x: Int): Int = x
}
