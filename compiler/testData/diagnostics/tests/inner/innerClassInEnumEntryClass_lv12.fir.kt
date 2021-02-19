// !LANGUAGE: +InnerClassInEnumEntryClass -NestedClassesInEnumEntryShouldBeInner

enum class Enum {
    ENTRY_WITH_CLASS {
        inner class TestInner

        <!NESTED_CLASS_NOT_ALLOWED!>class TestNested<!>

        <!NESTED_CLASS_NOT_ALLOWED!>interface TestInterface<!>

        object TestObject

        enum class TestEnumClass {
            OTHER_ENTRY
        }

        companion object {}
    }
}
