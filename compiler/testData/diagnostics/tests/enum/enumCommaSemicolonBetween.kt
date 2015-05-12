enum class MyEnum {
    // Should be comma instead of semicolon
    <!ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER!>A<!><!SYNTAX!>;<!>
    B,
    <!ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER!>C<!><!SYNTAX!>;<!>
    // Semicolon missed, comma is optional
    <!ENUM_ENTRY_USES_DEPRECATED_OR_NO_DELIMITER!>D<!>,

    fun foo() = 0
}