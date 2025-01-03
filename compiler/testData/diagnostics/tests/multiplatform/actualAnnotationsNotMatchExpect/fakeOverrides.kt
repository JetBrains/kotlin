// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    @Ann
    open fun noAnnotationOnActual() {}
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>FakeOverrideExpect<!> : A

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I<!> {
    fun noAnnotationOnActual()
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>FakeOverrideActual<!> : I {
    @Ann
    override fun noAnnotationOnActual()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class FakeOverrideExpect : A() {
    override fun noAnnotationOnActual() {}
}

abstract class Intermediate : I {
    override fun noAnnotationOnActual() {}
}

actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>FakeOverrideActual<!> : Intermediate(), I
