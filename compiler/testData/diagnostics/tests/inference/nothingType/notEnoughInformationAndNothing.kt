// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE
// COMPARE_WITH_LIGHT_TREE

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
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    )
    select(materialize(), materialize<String>())
    select(materialize(), null, Inv<String>())
    <!TYPE_MISMATCH!>select<!>(
        materialize(),
        null
    )
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>select<!>(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
    )
    select(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>(),
        null
    )
}
