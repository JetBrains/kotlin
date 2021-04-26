// !LANGUAGE: +InnerClassInEnumEntryClass -NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        inner class TestInner

        <!NESTED_CLASS_NOT_ALLOWED!>class TestNested<!>

        <!LOCAL_INTERFACE_NOT_ALLOWED, NESTED_CLASS_NOT_ALLOWED!>interface TestInterface<!>

        <!LOCAL_OBJECT_NOT_ALLOWED!>object TestObject<!>

        enum class TestEnumClass {
            OTHER_ENTRY
        }

        companion object {}
    }
}
