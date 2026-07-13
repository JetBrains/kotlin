// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// RENDER_DIAGNOSTIC_ARGUMENTS
abstract class X(val y: Bar)

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object Bar<!> {
    <!POSSIBLY_UNINITIALIZED_PROPERTY!>val prop = <!ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS!>Foo.const<!><!>
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>class Foo {
    companion object : X(Bar) {
        val const = "AAA"
    }
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, objectDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral */
