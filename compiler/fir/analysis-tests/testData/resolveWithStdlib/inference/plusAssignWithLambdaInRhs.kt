// ISSUE: KT-39005
// !DUMP_CFG

fun test() {
    val list: MutableList<(String) -> String> = null!!
    <!AMBIGUITY!>list += { <!UNRESOLVED_REFERENCE!>it<!> }<!>
}

class A<T>(private val executor: ((T) -> Unit) -> Unit)

fun <T> postpone(computation: () -> T): A<T> {
    val queue = mutableListOf<() -> Unit>()

    return A { resolve ->
        <!AMBIGUITY!>queue += {
            resolve(computation())
        }<!>
    }
}
