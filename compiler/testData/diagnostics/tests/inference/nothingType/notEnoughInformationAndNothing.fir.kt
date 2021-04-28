// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

class Inv<I>
fun <T> materialize(): Inv<T> = TODO()
fun <K> id(arg: K) = arg
fun <S> select(vararg args: S): S = TODO()

fun test1(b: Boolean?) {
    val v = when(b) {
        true -> materialize()
        false -> null
        null -> materialize<String>()
    }
    v checkType { _<Inv<String>?>() }
}

fun test2() {
    <!NEW_INFERENCE_ERROR!>select(
        materialize()
    )<!>
    select(materialize(), materialize<String>())
    select(materialize(), null, Inv<String>())
    <!NEW_INFERENCE_ERROR!>select(
        materialize(),
        null
    )<!>
    <!NEW_INFERENCE_ERROR!>select(
        materialize(),
        materialize()
    )<!>
    select(
        materialize(),
        materialize(),
        null
    )
}
