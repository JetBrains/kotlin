// RUN_PIPELINE_TILL: FRONTEND

class A(val city: List<String><!SYNTAX!><!> field: MutableList<String> = mutableListOf())

fun local() {
    val local: List<String> <!SYNTAX!>field: MutableList<String> = mutableListOf()<!>
}

fun localInWhen() {
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val local: List<String><!><!SYNTAX!><!> <!UNRESOLVED_REFERENCE!>field<!> <!SYNTAX!>=<!> mutableListOf(<!SYNTAX!><!>)<!SYNTAX!>)<!> {     //SYNTAX
        <!SYNTAX!>is<!> <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>List<!><*> <!SYNTAX!>-> "1"<!>
        <!SYNTAX!>else<!> <!SYNTAX!>-> "2"<!>
    }
}

fun destructuringDeclaration() {
    <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (a, b)<!> <!SYNTAX!>field = Pair("", "")<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, functionDeclaration, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration, whenExpression, whenWithSubject */
