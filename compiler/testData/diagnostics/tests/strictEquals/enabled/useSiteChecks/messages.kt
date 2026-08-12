// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface H<T> {
    override fun equals(@EqualityBound(H::class) other: Any?): Boolean
}

interface K {
    override fun equals(@EqualityBound(K::class) other: Any?): Boolean
}

class G<T> {
    override fun equals(other: Any?): Boolean = true
}

fun test(g: G<String>, h: H<String>, k: K): Boolean {
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>g == h<!>) return true
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>h == g<!>) return true
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>h == k<!>) return true
    return false
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override, typeParameter */
