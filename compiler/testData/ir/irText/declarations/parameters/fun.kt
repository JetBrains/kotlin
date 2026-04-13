fun <T> test1(i: Int, j: T) {}

fun test2(i: Int = 0, j: String = "") {}

fun test3(vararg args: String) {}

fun String.textExt1(i: Int, j: String) {}

class Host {
    fun String.testMembetExt1(i: Int, j: String) {}

    fun <T> String.testMembetExt2(i: Int, j: T) {}
}