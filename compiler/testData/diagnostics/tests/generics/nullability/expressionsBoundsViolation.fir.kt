// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T : CharSequence>(x: T)

fun <E : CharSequence> foo1(x: E) {}
fun <E : CharSequence> E.foo2() {}

fun <F : String?> bar(x: F) {
    <!CANNOT_INFER_PARAMETER_TYPE!>A<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    <!INAPPLICABLE_CANDIDATE!>A<!><<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    <!CANNOT_INFER_PARAMETER_TYPE!>foo1<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    <!INAPPLICABLE_CANDIDATE!>foo1<!><<!UPPER_BOUND_VIOLATED!>F<!>>(x)

    x<!UNSAFE_CALL!>.<!>foo2()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo2<!><F>()
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, funWithExtensionReceiver, functionDeclaration, nullableType,
primaryConstructor, typeConstraint, typeParameter */
