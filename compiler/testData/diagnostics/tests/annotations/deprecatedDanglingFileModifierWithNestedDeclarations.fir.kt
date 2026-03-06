// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72740

annotation class Anno(val s: String)

@Deprecated("Use 'AAA' instead"
<!UNRESOLVED_REFERENCE!>open<!> <!EXPRESSION_EXPECTED!>class MyClass : Any() {
    val foo = 24

    @Anno("str")
    fun baz() {

    }

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {
        @Anno("something")
        fun getSomething(a: Int = 24) {

        }
    }
}<!><!SYNTAX, SYNTAX!><!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, functionDeclaration, integerLiteral,
localClass, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
