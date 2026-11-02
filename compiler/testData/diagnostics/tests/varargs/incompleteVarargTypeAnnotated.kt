// RUN_PIPELINE_TILL: FRONTEND
annotation class Anno

fun annotatedIncompleteType(vararg p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!><!>) {}
fun annotatedIncompleteUnresolvedType(vararg p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> <!UNRESOLVED_REFERENCE!>Unresolved<!>.<!SYNTAX!><!>) {}

class Constructor(vararg p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!><!>)
class ConstructorProperty(vararg val p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!><!>)

fun localDeclarations() {
    fun local(vararg p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!><!>) {}
    val anonymous = fun(vararg p: <!WRONG_ANNOTATION_TARGET, WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!><!>) {}
}

fun nonVarargAnnotatedIncompleteType(p: <!WRONG_ANNOTATION_TARGET!>@Anno<!> Int.<!SYNTAX!><!>) {}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, classDeclaration, functionDeclaration, localFunction,
localProperty, outProjection, primaryConstructor, propertyDeclaration, vararg */
