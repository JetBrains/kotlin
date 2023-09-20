// ISSUE: KT-59565

@Target(AnnotationTarget.TYPE)
annotation class Ann(val x: Int)

class A<T: @Ann(<!ARGUMENT_TYPE_MISMATCH!>{
    fun local() = 1
    var result = local()
    result += 1
    result
}<!>) Any>

fun f(x: @Ann(<!ARGUMENT_TYPE_MISMATCH!>{
    fun local() = 1
    var result = local()
    result += 1
    result
}<!>) Int) = x
