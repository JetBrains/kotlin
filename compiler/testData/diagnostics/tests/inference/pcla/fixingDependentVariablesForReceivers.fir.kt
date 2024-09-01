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
        prop.call("")
    }.length

    generate {
        id(prop).call("")
    }.length

    generate {
        prop.call("")

        get().<!UNRESOLVED_REFERENCE!>length<!>

        id(get()).length
    }.length

    generate {
        select("", get()).length
    }.length
}