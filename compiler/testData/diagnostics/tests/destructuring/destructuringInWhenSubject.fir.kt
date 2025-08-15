// RUN_PIPELINE_TILL: FRONTEND
data class Foo(val name: String)

fun main() {
    val foo = Foo("John")
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val (name) = foo<!>) {
        <!USELESS_IS_CHECK!>is String<!> -> bar("1")
        <!USELESS_IS_CHECK!>is Foo<!> -> bar("2")
        else -> bar(<!UNRESOLVED_REFERENCE!>name<!>)
    }
}

fun main2() {
    val foo = Foo("John")
    when (<!ILLEGAL_DECLARATION_IN_WHEN_SUBJECT!>val (name) = foo<!>) {
        <!USELESS_IS_CHECK!>is String<!> -> bar("1")
        <!USELESS_IS_CHECK!>is Foo<!> -> bar("2")
    }
}

fun bar(x: Any) {}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, isExpression, localProperty, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
