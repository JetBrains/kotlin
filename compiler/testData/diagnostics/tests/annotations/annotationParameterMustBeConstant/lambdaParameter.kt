// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59565
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

class A<T: @Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>{
    fun local() = 1
    var result = local()
    result += 1
    result
}<!>) Any>

fun f(x: @Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_MISMATCH!>{
    fun local() = 1
    var result = local()
    result += 1
    result
}<!>) Int) = x
