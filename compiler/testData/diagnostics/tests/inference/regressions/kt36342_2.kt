fun <K> id(arg: K): K = arg
fun <M> materialize(): M = TODO()

fun test(b: Boolean) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(if (b) {
        id(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } else {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(
            <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>materialize<!>()
        )
    })
}
