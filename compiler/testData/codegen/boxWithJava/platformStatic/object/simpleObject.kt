import kotlin.jvm.JvmStatic

object A {

    val b: String = "OK"

    @JvmStatic val c: String = "OK"

    @JvmStatic fun test1() = b

    @JvmStatic fun test2() = b

    @JvmStatic fun String.test3() = this + b
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    if (Test.test3() != "JAVAOK") return "fail 3"

    if (Test.test4() != "OK") return "fail 4"

    return "OK"
}