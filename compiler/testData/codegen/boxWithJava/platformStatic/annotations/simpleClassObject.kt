import kotlin.jvm.JvmStatic

@Retention(AnnotationRetention.RUNTIME)
annotation class testAnnotation

class A {

    companion object {
        val b: String = "OK"

        @JvmStatic @testAnnotation fun test1() = b
    }
}

object B {
    val b: String = "OK"

    @JvmStatic @testAnnotation fun test1() = b
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    return "OK"
}