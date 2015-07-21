import kotlin.platform.platformStatic

annotation(retention = AnnotationRetention.RUNTIME) class testAnnotation

class A {

    companion object {
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