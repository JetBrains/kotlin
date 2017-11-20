// !LANGUAGE: -InnerClassInEnumEntryClass -NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>inner<!> class TestInner

        <!NESTED_CLASS_DEPRECATED!>class TestNested<!>

        <!NESTED_CLASS_DEPRECATED!>interface TestInterface<!>

        <!NESTED_CLASS_DEPRECATED!>object TestObject<!>

        <!NESTED_CLASS_DEPRECATED!>enum class TestEnumClass<!> {
            OTHER_ENTRY
        }

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}
