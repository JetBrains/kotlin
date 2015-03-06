import kotlin.platform.platformStatic

class A {

    default object {
        val b: String = "OK"

        platformStatic val c: String = "OK"

        platformStatic fun test1() = b

        platformStatic fun test2() = b

        platformStatic fun String.test3() = this + b
    }
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    if (Test.test3() != "JAVAOK") return "fail 3"

    if (Test.test4() != "OK") return "fail 4"

    return "OK"
}