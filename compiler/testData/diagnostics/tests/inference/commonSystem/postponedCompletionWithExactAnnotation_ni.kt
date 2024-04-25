// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNCHECKED_CAST

interface ISample

fun <K> elvisSimple(x: K?, y: K): K = y

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "HIDDEN")
fun <K> elvisExact(x: K?, y: K): @kotlin.internal.Exact K = y

fun <T : Number> materialize(): T? = null
fun <T> Any?.materialize(): T = null as T

fun test(nullableSample: ISample, any: Any) {
    <!DEBUG_INFO_EXPRESSION_TYPE("ISample?")!>elvisSimple(
        nullableSample,
        <!DEBUG_INFO_EXPRESSION_TYPE("{ISample & Number}?")!>materialize()<!>
    )<!>

    elvisSimple(
        <!DEBUG_INFO_EXPRESSION_TYPE("ISample?")!>elvisSimple(nullableSample, materialize())<!>,
        any
    )

    elvisSimple(
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>elvisExact(nullableSample, materialize())<!>,
        any
    )

    val a: String? = null

    val x1: String? = run {
        a ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>a?.materialize()<!>
    }

    val x2 = run {
        a ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>a?.materialize()<!>
    }
}
