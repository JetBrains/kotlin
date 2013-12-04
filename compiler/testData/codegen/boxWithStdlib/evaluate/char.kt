package test

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(val c1: Int)

Ann('a' - 'a') class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.c1 != 0) return "fail : expected = ${1}, actual = ${annotation.c1}"
    return "OK"
}
