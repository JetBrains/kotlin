// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76140

const val s = "asd"

interface I

open class A<T : I>(val s: String)

class B : A<<!UPPER_BOUND_VIOLATED!>Any<!>>(s)

/* GENERATED_FIR_TAGS: classDeclaration, const, interfaceDeclaration, primaryConstructor, propertyDeclaration,
stringLiteral, typeConstraint, typeParameter */
