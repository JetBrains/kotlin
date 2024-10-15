// RUN_PIPELINE_TILL: SOURCE
class Outer {
    inner class Inner {
        <!NESTED_CLASS_NOT_ALLOWED("Interface")!>interface TestNestedInterface<!>
    }
}