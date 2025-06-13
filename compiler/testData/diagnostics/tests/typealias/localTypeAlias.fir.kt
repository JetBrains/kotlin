// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun <T> emptyList(): List<T> = null!!

fun <T> foo() {
    <!UNSUPPORTED!>typealias LT = List<T><!>

    val a: <!UNRESOLVED_REFERENCE!>LT<!> = <!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>()

    fun localFun(): <!UNRESOLVED_REFERENCE!>LT<!> {
        <!UNSUPPORTED!>typealias LLT = List<T><!>

        val b: <!UNRESOLVED_REFERENCE!>LLT<!> = a

        return b
    }

    localFun()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, localFunction, localProperty, nullableType,
propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
