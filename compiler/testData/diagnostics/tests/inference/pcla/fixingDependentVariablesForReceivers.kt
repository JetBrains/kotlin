// RUN_PIPELINE_TILL: FRONTEND
fun <E> generate(c: Controller<E>.() -> Unit): E = TODO()

interface In<in T1> {
    fun call(t: T1) {}
}

interface Controller<F> {
    val prop: In<F>
    fun get(): F
}

fun <F> id(f: F): F = TODO()
fun <F2> select(e1: F2, e: F2): F2 = e

fun main() {
    generate {
        // No non-trivial constraints, thus cannot be semi-fixed
        get().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>
        get().toString()

        prop.call("")
    }

    generate {
        prop.call("")
    }.length

    generate {
        id(prop).call("")
    }.length

    generate {
        prop.call("")

        get().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>

        id(get()).<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>
    }.length

    generate {
        select("", get()).length
    }.length
}
