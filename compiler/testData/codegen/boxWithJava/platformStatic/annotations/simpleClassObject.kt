import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.platform.platformStatic

Retention(RetentionPolicy.RUNTIME)
annotation class testAnnotation

class A {

    default object {
        val b: String = "OK"

        platformStatic testAnnotation fun test1() = b
    }
}

object B {
    val b: String = "OK"

    platformStatic testAnnotation fun test1() = b
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    return "OK"
}