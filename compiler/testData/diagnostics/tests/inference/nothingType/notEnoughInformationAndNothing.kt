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
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    )
    select(materialize(), materialize<String>())
    select(materialize(), null, Inv<String>())
    <!TYPE_MISMATCH!>select<!>(
        materialize(),
        null
    )
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    )
    select(
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        null
    )
}
