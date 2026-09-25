// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

@Deprecated("", level = DeprecationLevel.HIDDEN)
open class Foo

fun test(f: <!UNRESOLVED_REFERENCE!>Foo<!>) {
    f.toString()
    val g: <!UNRESOLVED_REFERENCE!>Foo<!>? = <!UNRESOLVED_REFERENCE!>Foo<!>()
}

class Bar : <!UNRESOLVED_REFERENCE!>Foo<!>()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, propertyDeclaration,
stringLiteral */
