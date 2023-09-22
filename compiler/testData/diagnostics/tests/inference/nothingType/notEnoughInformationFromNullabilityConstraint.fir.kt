// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <M> make(): M? = null
fun <I> id(arg: I): I = arg
fun <S> select(vararg args: S): S = TODO()

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>make<!>()
    )

    select(make(), null)

    if (true) make() else TODO()
}
