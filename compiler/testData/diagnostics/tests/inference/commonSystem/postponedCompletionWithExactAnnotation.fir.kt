// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: -NewInference

interface ISample

fun <K> elvisSimple(x: K?, y: K): K = y

@Suppress("INVISIBLE_REFERENCE")
fun <K> elvisExact(x: K?, y: K): @kotlin.internal.Exact K = y

fun <T : Number> materialize(): T? = TODO()

fun test(nullableSample: ISample, any: Any) {
    elvisSimple(
        nullableSample,
        materialize()
    )

    elvisSimple(
        elvisSimple(nullableSample, materialize()),
        any
    )

    elvisSimple(
        elvisExact(nullableSample, materialize()),
        any
    )
}
