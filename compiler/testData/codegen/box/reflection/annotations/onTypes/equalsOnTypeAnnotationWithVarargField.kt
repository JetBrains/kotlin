// TARGET_BACKEND: JVM
// WITH_REFLECT
// LAMBDAS: CLASS

import kotlin.reflect.jvm.reflect

@Target(AnnotationTarget.TYPE)
annotation class Fee(vararg val i: Int)

fun box(): String {
    val f1 = { i: @Fee(0) Int -> }
    val f2 = { s: @Fee(0) String -> }

    val f1ParamTypeAnnotations = f1.reflect()!!.parameters.first().type.annotations
    val f2ParamTypeAnnotations = f2.reflect()!!.parameters.first().type.annotations

    return if (f1ParamTypeAnnotations == f2ParamTypeAnnotations) return "OK" else "Fail"
}