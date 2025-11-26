// RUN_PIPELINE_TILL: FRONTEND
data class Foo(val name: String)

fun main() {
    val foo = Foo("John")
    when (<!DECLARATION_IN_ILLEGAL_CONTEXT!>val (name) = <!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!>) {
        is String -> bar("1")
        is Foo -> bar("2")
        else -> bar(<!UNRESOLVED_REFERENCE!>name<!>)
    }
}

fun main2() {
    val foo = Foo("John")
    when (<!DECLARATION_IN_ILLEGAL_CONTEXT!>val (name) = <!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!>) {
        is String -> bar("1")
        is Foo -> bar("2")
    }
}

fun bar(x: Any) {}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, isExpression, localProperty, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
