// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

class Inv<I>
fun <T> materialize(): Inv<T> = TODO()
fun <K> id(arg: K) = arg
fun <S> select(vararg args: <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>S<!>): S = TODO()

fun test1(b: Boolean?) {
    val v = when(b) {
        true -> materialize()
        false -> null
        null -> materialize<String>()
    }
    v checkType { _<Inv<String>?>() }
}

fun test2() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    )
    select(materialize(), materialize<String>())
    select(materialize(), null, Inv<String>())
    <!NEW_INFERENCE_ERROR!>select(
        materialize(),
        null
    )<!>
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        materialize()
    )
    select(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        materialize(),
        null
    )
}
