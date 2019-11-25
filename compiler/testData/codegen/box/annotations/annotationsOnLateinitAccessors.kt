// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// TARGET_BACKEND: JVM

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