enum class MixedEnum {
    ENTRY1;
    companion object {
        val first = 1
    }
    <!ENUM_ENTRY_AFTER_ENUM_MEMBER!>ENTRY2<!>;
    fun foo(): String = "xyz"
    <!ENUM_ENTRY_AFTER_ENUM_MEMBER!>ENTRY3<!>
}