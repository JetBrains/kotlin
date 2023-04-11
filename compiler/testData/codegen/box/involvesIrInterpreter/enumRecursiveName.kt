// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR

enum class TestEnum(val testNaming: String) {
    OK(OK.<!EVALUATED("OK")!>name<!>),
}

fun box(): String {
    val name = TestEnum.OK.<!EVALUATED("OK")!>name<!>
    return name
}
