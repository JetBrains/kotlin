// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <M> make(): M? = null
fun <I> id(arg: I): I = arg
fun <S> select(vararg args: S): S = TODO()

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(
        <!CANNOT_INFER_PARAMETER_TYPE!>make<!>()
    )

    select(make(), null)

    if (true) make() else TODO()
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, nullableType, typeParameter, vararg */
