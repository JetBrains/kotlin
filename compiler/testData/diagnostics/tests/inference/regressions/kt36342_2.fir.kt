// !WITH_NEW_INFERENCE

fun <K> id(arg: K): K = arg
fun <M> materialize(): M = TODO()

fun test(b: Boolean) {
    id(if (b) {
        id(<!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>unresolved<!>)
    } else {
        id(
            materialize()
        )
    })
}
