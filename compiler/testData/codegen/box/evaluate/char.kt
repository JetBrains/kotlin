// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

package test

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val c1: Int)

@Ann('a' - 'a') class MyClass

fun box(): String {
    val annotation = MyClass::class.java.getAnnotation(Ann::class.java)!!
    if (annotation.c1 != 0) return "fail : expected = ${1}, actual = ${annotation.c1}"
    return "OK"
}
