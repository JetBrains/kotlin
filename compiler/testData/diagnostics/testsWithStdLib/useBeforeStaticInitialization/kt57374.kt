// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class Base {
    companion object {
        <!POSSIBLY_UNINITIALIZED_PROPERTY!>val fooAccess = <!ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS!>Derived.foo()<!><!>
    }
}<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class Derived(var value: String) : Base() {
    companion object {
        fun foo(): String = "foo"
    }
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, primaryConstructor,
propertyDeclaration, sealed, stringLiteral */
