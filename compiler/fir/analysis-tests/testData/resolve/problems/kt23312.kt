// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-23312

// KT-23312: Type inference with regard to expected type and 'Exact' constraints

// This function stands for usual elvis operator
@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <T> elvis(x: T?, y: T): @kotlin.internal.Exact T = if (x != null) x else y

fun foo() {
    var local: String? = null
    local = elvis("non-null", "non-null")

    val explicit: String? = elvis("non-null", "non-null")
}

/* GENERATED_FIR_TAGS: assignment, dnnType, equalityExpression, functionDeclaration, ifExpression, localProperty,
nullableType, propertyDeclaration, smartcast, stringLiteral, typeParameter */
