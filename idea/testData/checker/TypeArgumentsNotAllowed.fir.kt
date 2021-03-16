package foo

open class A<T>

fun <T> f(t: <error descr="[TYPE_ARGUMENTS_NOT_ALLOWED] Type arguments are not allowed for type parameters">T<T></error>) {}

fun <T> use(b: <error descr="[WRONG_NUMBER_OF_TYPE_ARGUMENTS] One type argument expected for foo/A">foo<T>.A<T></error>) {}
