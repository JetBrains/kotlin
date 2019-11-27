// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT

// TARGET_BACKEND: JVM

// FILE: JavaAnn.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String[] value() default {};
    String[] path() default {};
}

// FILE: test.kt

import java.util.Arrays
import kotlin.reflect.KClass
import kotlin.reflect.KFunction0
import kotlin.reflect.full.findAnnotation

inline fun <reified T : Annotation> test(kFunction: KFunction0<Unit>, test: T.() -> Unit) {
    val annotation = kFunction.findAnnotation<T>()!!
    annotation.test()
}

fun check(b: Boolean, message: String) {
    if (!b) throw RuntimeException(message)
}

annotation class Ann(vararg val s: String)

@Ann(s = ["value1", "value2"])
fun test1() {}

@Ann(s = arrayOf("value3", "value4"))
fun test2() {}

@JavaAnn(value = ["value5"], path = ["value6"])
fun test3() {}

@JavaAnn("value7", path = ["value8"])
fun test4() {}

fun box(): String {
    test<Ann>(::test1) {
        check(s.contentEquals(arrayOf("value1", "value2")), "Fail 1: ${s.joinToString()}")
    }

    test<Ann>(::test2) {
        check(s.contentEquals(arrayOf("value3", "value4")), "Fail 2: ${s.joinToString()}")
    }

    test<JavaAnn>(::test3) {
        check(value.contentEquals(arrayOf("value5")), "Fail 3: ${value.joinToString()}")
        check(path.contentEquals(arrayOf("value6")), "Fail 3: ${path.joinToString()}")
    }

    test<JavaAnn>(::test4) {
        check(value.contentEquals(arrayOf("value7")), "Fail 4: ${value.joinToString()}")
        check(path.contentEquals(arrayOf("value8")), "Fail 4: ${path.joinToString()}")
    }

    return "OK"
}
