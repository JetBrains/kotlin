// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.
annotation class TestAnn(val x: String)

enum class TestEnum {
    @TestAnn("ENTRY1") ENTRY1,
    @TestAnn("ENTRY2") ENTRY2 {
        val x = 42
    }
}
