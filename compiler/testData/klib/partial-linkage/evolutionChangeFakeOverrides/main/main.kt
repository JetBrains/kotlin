import serialization.fake_overrides.*
fun test1() = Z().bar()

fun box(): String {
    val failedTests = listOfNotNull(
        test0().takeIf { it != "barMoved" },
        test1().takeIf { it != "barMoved" },
        test2().takeIf { it != "quxChild" },
        test3().takeIf { it != "quxSuper" },
        test4().takeIf { it != "ticSuper" },
        test5().takeIf { it != "ticSuper" },
    )
    return if (failedTests.isNotEmpty()) failedTests.joinToString() else "OK"
}
