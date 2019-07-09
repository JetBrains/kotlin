class Outer {
    inner class Inner {
        <!NESTED_CLASS_NOT_ALLOWED("Enum class")!>enum class TestNestedEnum<!>
    }
}