enum class TestEnum {
    ENTRY;

    <!VIRTUAL_MEMBER_HIDDEN!>fun getDeclaringClass()<!> {}
    <!VIRTUAL_MEMBER_HIDDEN!>fun finalize()<!> {}
}

class TestFinalize {
    fun finalize() {}
}
