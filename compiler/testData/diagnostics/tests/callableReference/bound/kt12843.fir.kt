// RUN_PIPELINE_TILL: FRONTEND
class Foo {
    fun bar() {}
    fun f() = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::bar
}

val f: () -> Unit = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::foo

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, propertyDeclaration */
