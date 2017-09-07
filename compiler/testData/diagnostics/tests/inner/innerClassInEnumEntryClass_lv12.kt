// !LANGUAGE: +InnerClassInEnumEntryClass

enum class Enum {
    ENTRY_WITH_CLASS {
        inner class TestInner

        class TestNested

        interface TestInterface

        object TestObject

        enum class TestEnumClass {
            OTHER_ENTRY
        }

        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {}
    }
}
