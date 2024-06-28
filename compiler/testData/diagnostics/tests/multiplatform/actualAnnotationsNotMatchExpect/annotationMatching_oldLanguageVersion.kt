// FIR_IDENTICAL
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
expect class AnnotationOnExpectOnly

expect class AnnotationInside {
    @Ann
    fun onlyOnExpect()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class AnnotationOnExpectOnly

actual class AnnotationInside {
    actual fun onlyOnExpect() {}
}
