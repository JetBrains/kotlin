package test

enum class EnumTest(val i: Int = 0) {
    ENTRY1: EnumTest()
    ENTRY2: EnumTest(1)
}

class EAnotherClass