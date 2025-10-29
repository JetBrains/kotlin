// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NameBasedDestructuring +EnableNameBasedDestructuringShortForm

data class HasDeprecation(@Deprecated("") val x: String)

data class D(val x: String)

fun baz(foo: HasDeprecation) {
    <!WRONG_ANNOTATION_TARGET!>@Suppress("DEPRECATION")<!>
    val (<!DEPRECATION!>x<!>) = foo

    <!WRONG_ANNOTATION_TARGET!>@Suppress("DEPRECATION")<!>
    (val y = <!DEPRECATION!>x<!>) = foo

    <!WRONG_ANNOTATION_TARGET!>@Suppress("UNCHECKED_CAST")<!>
    val (z = x) = Any() as D

    <!WRONG_ANNOTATION_TARGET!>@Suppress("UNCHECKED_CAST")<!>
    (val z2 = x) = Any() as D

    <!WRONG_ANNOTATION_TARGET!>@Suppress("UNCHECKED_CAST")<!>
    val [_] = Any() as D

    <!WRONG_ANNOTATION_TARGET!>@Suppress("UNCHECKED_CAST")<!>
    [val _] = Any() as D
}

/* GENERATED_FIR_TAGS: annotationDeclaration, assignment, classDeclaration, data, destructuringDeclaration,
functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
