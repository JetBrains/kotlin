annotation class TestAnn(val x: String)

enum class TestEnum {
    @TestAnn("ENTRY1") ENTRY1,
    @TestAnn("ENTRY2") ENTRY2 {
        val x = 42
    }
}