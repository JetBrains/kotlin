// TARGET_BACKEND: JVM_IR
// IGNORE_FIR_DIAGNOSTICS
// IGNORE_BACKEND_K1: JVM_IR
// !DIAGNOSTICS: -UNINITIALIZED_ENUM_ENTRY

enum class TestEnum(val testNaming: String) {
    OK(OK.name),
}

const val name = TestEnum.OK.name

fun box(): String {
    return name
}
