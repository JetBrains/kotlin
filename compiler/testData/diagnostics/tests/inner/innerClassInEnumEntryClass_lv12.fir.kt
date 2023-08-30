// !LANGUAGE: +InnerClassInEnumEntryClass -NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        inner class TestInner

        <!NESTED_CLASS_NOT_ALLOWED!>class TestNested<!>

        <!NESTED_CLASS_NOT_ALLOWED!>interface TestInterface<!>

        <!LOCAL_OBJECT_NOT_ALLOWED!>object TestObject<!>

        <!WRONG_MODIFIER_TARGET!>enum<!> class TestEnumClass {
            OTHER_ENTRY
        }

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}
