package test

annotation(retention = AnnotationRetention.RUNTIME) class Ann(val c1: Int)

Ann('a' - 'a') class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.c1 != 0) return "fail : expected = ${1}, actual = ${annotation.c1}"
    return "OK"
}
