// RUN_PIPELINE_TILL: BACKEND
// SKIP_FIR_DUMP

import kotlin.annotation.AnnotationTarget.*

@Target(TYPEALIAS)
annotation class TATarget

class Class {
    @TATarget
    typealias Annotated1 = String

    @Deprecated (message = "test")
    typealias Annotated2 = String
}

fun accessAnnotatedTA() {
    var instanceOfAnnotated1TA: String = Class.Annotated1()
    var instanceOfAnnotated2TA: String = Class.<!DEPRECATION!>Annotated2<!>()
}
