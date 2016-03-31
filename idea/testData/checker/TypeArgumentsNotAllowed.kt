package foo

open class A<T>

fun <T> f(<warning>t</warning>: T<error descr="[TYPE_ARGUMENTS_NOT_ALLOWED] Type arguments are not allowed for type parameters"><T></error>) {}

fun <T> use(<warning>b</warning>: foo<error descr="[TYPE_ARGUMENTS_NOT_ALLOWED] Type arguments are not allowed here"><T></error>.A<T>) {}