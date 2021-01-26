// !WITH_NEW_INFERENCE

fun <K> id(arg: K): K = arg
fun <M> materialize(): M = TODO()

fun test(b: Boolean) {
    <!INAPPLICABLE_CANDIDATE!>id<!>(if (b) {
        <!INAPPLICABLE_CANDIDATE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } else {
        id(
            materialize()
        )
    })
}
