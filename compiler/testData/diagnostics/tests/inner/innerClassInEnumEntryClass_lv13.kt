// !LANGUAGE: +InnerClassInEnumEntryClass +NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        inner class TestInner

        <!NESTED_CLASS_NOT_ALLOWED!>class TestNested<!>

        <!NESTED_CLASS_NOT_ALLOWED!>interface TestInterface<!>

        <!NESTED_CLASS_NOT_ALLOWED!>object TestObject<!>

        <!NESTED_CLASS_NOT_ALLOWED!>enum class TestEnumClass<!> {
            OTHER_ENTRY
        }

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}
