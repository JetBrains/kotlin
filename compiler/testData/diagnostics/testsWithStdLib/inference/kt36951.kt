// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Base<T : <!CYCLIC_GENERIC_UPPER_BOUND!>T<!>> : HashSet<T>() {
    fun foo() {
        super.remove(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral, superExpression, typeConstraint,
typeParameter */
