package test

@Repeatable
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
annotation class Anno(val code: Int)

@Anno(1)
@Anno(2)
class Z

@Anno(3)
@Anno(4)
fun f() {}

@Anno(5)
@Anno(6)
typealias S = String
