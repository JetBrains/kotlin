enum class MixedEnum {
    companion object {
        val first = 1
    }
    fun foo(): String = "xyz"
    <!ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER, ENUM_ENTRY_AFTER_ENUM_MEMBER!>ENTRY1<!>
    <!ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER!>ENTRY2<!>
    ENTRY3
}