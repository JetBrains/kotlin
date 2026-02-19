// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// Issue: KT-30734

class Sample {
    fun foo(): Boolean = true
}

fun test(ls: Sample?) {
    val filter: () -> Boolean = if (ls == null) {
        { false }
    } else {
        { <!DEBUG_INFO_SMARTCAST!>ls<!>.foo() } // OK in OI, error in NI
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast */
