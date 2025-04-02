// WITH_REFLECT
// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-69075

// Please make sure that this test is consistent with the diagnostic test "annotationsTargetingLateinitAccessor.kt"

import kotlin.reflect.KAnnotatedElement

annotation class Ann

fun check(element: KAnnotatedElement, annotationExists: Boolean) {
    require(element.annotations.isNotEmpty() == annotationExists) { "Fail: $element" }
}

class LateinitProperties {
    @get:Ann
    lateinit var x0: String

    @get:Ann
    private lateinit var x1: String

    fun test() {
        check(::x0.getter, annotationExists = true)
        check(::x1.getter, annotationExists = false)
    }
}

fun box(): String {
    LateinitProperties().test()
    return "OK"
}

