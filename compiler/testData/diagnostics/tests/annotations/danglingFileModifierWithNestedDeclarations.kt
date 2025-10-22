// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72740

annotation class Anno(val s: String)

@Anno("Use 'AAA' instead"
<!UNRESOLVED_REFERENCE!>open<!> <!DECLARATION_IN_ILLEGAL_CONTEXT!>class MyClass : Any() {
    val foo = 24

    <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Anno("str")<!>
    fun baz() {

    }

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {
        <!ANNOTATION_USED_AS_ANNOTATION_ARGUMENT!>@Anno("something")<!>
        fun getSomething(a: Int = 24) {

        }
    }
}<!><!SYNTAX, SYNTAX!><!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, functionDeclaration, integerLiteral,
localClass, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
