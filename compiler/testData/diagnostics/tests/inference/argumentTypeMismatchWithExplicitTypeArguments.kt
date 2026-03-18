// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

fun <S> select(arg1: S, arg2: S, arg3: S): S = arg1

fun main() {
    select<Number>(42L, 42.0, <!TYPE_MISMATCH!>Any()<!>)

    val nullableString: String? = ""
    select<Any>(42L, 42.0, <!TYPE_MISMATCH!>nullableString<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration, stringLiteral,
typeParameter */
