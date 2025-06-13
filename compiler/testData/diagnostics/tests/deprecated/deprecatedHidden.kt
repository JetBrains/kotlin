// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

@Deprecated("", level = DeprecationLevel.HIDDEN)
open class Foo

fun test(f: <!DEPRECATION_ERROR!>Foo<!>) {
    f.toString()
    val g: <!DEPRECATION_ERROR!>Foo<!>? = <!DEPRECATION_ERROR!>Foo<!>()
}

class Bar : <!DEPRECATION_ERROR!>Foo<!>()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, propertyDeclaration,
stringLiteral */
