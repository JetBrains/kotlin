// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

annotation class TestAnn(val x: String)

enum class TestEnum {
    @TestAnn("ENTRY1") ENTRY1,
    @TestAnn("ENTRY2") ENTRY2 {
        val x = 42
    }
}
