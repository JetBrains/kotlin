// !DIAGNOSTICS: -FUNCTION_DECLARATION_WITH_NO_NAME
class ClassB() {
    private inner class ClassC: <!SYNTAX!>super<!><!SYNTAX!>.<!>@ClassA()<!SYNTAX!><!> {
    }
}
