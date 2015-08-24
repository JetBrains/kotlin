import kotlin.jvm.jvmStatic

object A {

    val b: String = "OK"

    jvmStatic val c: String = "OK"

    jvmStatic fun test1() = b

    jvmStatic fun test2() = b

    jvmStatic fun String.test3() = this + b
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    if (Test.test3() != "JAVAOK") return "fail 3"

    if (Test.test4() != "OK") return "fail 4"

    return "OK"
}