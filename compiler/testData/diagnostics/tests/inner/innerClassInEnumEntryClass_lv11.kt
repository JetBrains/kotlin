// !LANGUAGE: -InnerClassInEnumEntryClass -NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> class TestInner

        <!NESTED_CLASS_NOT_ALLOWED_SINCE_1_3!>class TestNested<!>

        <!NESTED_CLASS_NOT_ALLOWED_SINCE_1_3!>interface TestInterface<!>

        <!NESTED_CLASS_NOT_ALLOWED_SINCE_1_3!>object TestObject<!>

        <!NESTED_CLASS_NOT_ALLOWED_SINCE_1_3!>enum class TestEnumClass<!> {
            OTHER_ENTRY
        }

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}
