// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun <T> emptyList(): List<T> = null!!

fun <T> foo() {
    <!UNSUPPORTED_FEATURE!>typealias LT = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>List<T><!><!>

    val a: LT = emptyList()

    fun localFun(): LT {
        <!UNSUPPORTED_FEATURE!>typealias LLT = <!TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS!>List<T><!><!>

        val b: LLT = a

        return b
    }

    localFun()
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, localFunction, localProperty, nullableType,
propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
