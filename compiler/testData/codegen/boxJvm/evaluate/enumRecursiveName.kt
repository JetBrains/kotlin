// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR

enum class TestEnum(val testNaming: String) {
    OK(OK.name),
}

fun box(): String {
    val name = TestEnum.OK.name
    return name
}
