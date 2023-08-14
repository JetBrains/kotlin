// !DIAGNOSTICS: -UNUSED_PARAMETER

interface ISample

fun <K> elvisSimple(x: K?, y: K): K = y

@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>, "INVISIBLE_MEMBER", "HIDDEN")
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
