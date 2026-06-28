// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
<!POSSIBLE_INITIALIZATION_DEADLOCK!>sealed class S {
    <!POSSIBLE_INITIALIZATION_DEADLOCK!>object O<!> : S()

    companion object {
        <!POSSIBLY_UNINITIALIZED_PROPERTY!>val x = foo(<!ACCESSING_POSSIBLY_INACCESSIBLE_OBJECT_REFERENCE!>O<!>)<!>
    }
}<!>

fun foo(o: S) = 42

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, nestedClass,
objectDeclaration, propertyDeclaration, sealed */
