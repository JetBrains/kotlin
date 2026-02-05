// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR

enum class TestEnum(val testNaming: String) {
    OK(OK.<!EVALUATED("OK")!>name<!>),
}

// STOP_EVALUATION_CHECKS
fun box(): String {
    val name = TestEnum.OK.name
    return name
}
