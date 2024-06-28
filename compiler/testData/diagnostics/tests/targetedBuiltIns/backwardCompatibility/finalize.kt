// FIR_IDENTICAL
enum class TestEnum {
    ENTRY;

    fun <!VIRTUAL_MEMBER_HIDDEN!>getDeclaringClass<!>() {}
    fun <!VIRTUAL_MEMBER_HIDDEN!>finalize<!>() {}
}

class TestFinalize {
    fun finalize() {}
}
