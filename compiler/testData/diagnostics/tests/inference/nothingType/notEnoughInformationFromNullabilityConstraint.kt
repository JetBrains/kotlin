// DIAGNOSTICS: -UNUSED_PARAMETER

fun <M> make(): M? = null
fun <I> id(arg: I): I = arg
fun <S> select(vararg args: S): S = TODO()

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>make<!>()
    )

    select(<!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>make<!>(), null)

    if (true) <!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>make<!>() else TODO()
}
