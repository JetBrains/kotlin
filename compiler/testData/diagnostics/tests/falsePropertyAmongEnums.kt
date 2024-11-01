// FIR_IDENTICAL
// LANGUAGE: -ProperUninitializedEnumEntryAccessAnalysis

fun test() {
    <!WRONG_MODIFIER_TARGET!>enum<!> class MyEnum {
        A;

        val someProperty = 10
    }

    MyEnum.<!UNINITIALIZED_ENUM_ENTRY!>A<!>.someProperty
}
