// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2

typealias Errs = E1 | E2
typealias R = Int | E1
typealias Nested = <!NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE!>String | R<!>

fun nested(): <!NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE!>String | R<!> = E1()

class C : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>R<!>
object O : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>Errs<!>

fun constructors() {
    <!UNRESOLVED_REFERENCE!>R<!>()
    <!UNRESOLVED_REFERENCE!>Errs<!>()
}

fun classLiterals() {
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>R::class<!>
    <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Errs::class<!>
}

fun mismatch(): R = <!RETURN_TYPE_MISMATCH!>""<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, objectDeclaration, stringLiteral,
typeAliasDeclaration */
