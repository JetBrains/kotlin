// FIR_DISABLE_LAZY_RESOLVE_CHECKS
enum class TestEnum {
    ENTRY;

    fun <!VIRTUAL_MEMBER_HIDDEN!>getDeclaringClass<!>() {}
    fun finalize() {}
}

class TestFinalize {
    fun finalize() {}
}
