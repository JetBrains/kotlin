// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

inline fun <reified T : CharSequence?> foo(y: Any?) {
    if (y is T?) {
        if (y != null) {
            bar(<!DEBUG_INFO_SMARTCAST!>y<!>)
        }
    }
}

fun bar(x: CharSequence) {}

/* GENERATED_FIR_TAGS: dnnType, equalityExpression, functionDeclaration, ifExpression, inline, isExpression,
nullableType, reified, smartcast, typeConstraint, typeParameter */
