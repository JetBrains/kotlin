// DIAGNOSTICS: -UNUSED_PARAMETER

interface ISample

fun <K> elvisSimple(x: K?, y: K): K = y

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "HIDDEN")
fun <K> elvisExact(x: K?, y: K): @kotlin.internal.Exact K = y

fun <T : Number> materialize(): T? = TODO()

fun test(nullableSample: ISample, any: Any) {
    <!DEBUG_INFO_EXPRESSION_TYPE("ISample?")!>elvisSimple(
        nullableSample,
        materialize()
    )<!>

    elvisSimple(
        <!DEBUG_INFO_EXPRESSION_TYPE("ISample?")!>elvisSimple(nullableSample, materialize())<!>,
        any
    )

    elvisSimple(
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>elvisExact(nullableSample, materialize())<!>,
        any
    )
}
