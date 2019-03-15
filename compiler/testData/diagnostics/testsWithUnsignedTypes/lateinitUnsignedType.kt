// !DIAGNOSTICS: -UNUSED_VARIABLE

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: UInt

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: UByte
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var c: UShort
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var d: ULong
}