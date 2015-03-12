// !DIAGNOSTICS: -REDECLARATION

class C {
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {

    }

    val<!SYNTAX!><!> : Int = 1

    class<!SYNTAX!><!> {}

    enum class<!SYNTAX!><!> {}
}

class C1<in<!SYNTAX!>><!><!SYNTAX!><!> {}

class C2(val<!SYNTAX!><!>) {}