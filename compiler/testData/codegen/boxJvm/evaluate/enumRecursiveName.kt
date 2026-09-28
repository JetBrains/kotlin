// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM

enum class TestEnum(val testNaming: String) {
    OK(OK.name),
}

fun box(): String {
    val name = TestEnum.OK.name
    return name
}
