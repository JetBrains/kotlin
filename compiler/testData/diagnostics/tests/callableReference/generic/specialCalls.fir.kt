// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun baz(i: Int) = i
fun <T> bar(x: T): T = TODO()

fun nullableFun(): ((Int) -> Int)? = null

fun test() {
    val x1: (Int) -> Int = bar(if (true) <!UNRESOLVED_REFERENCE!>::baz<!> else <!UNRESOLVED_REFERENCE!>::baz<!>)
    val x2: (Int) -> Int = bar(nullableFun() ?: <!UNRESOLVED_REFERENCE!>::baz<!>)
    val x3: (Int) -> Int = bar(::baz ?: <!UNRESOLVED_REFERENCE!>::baz<!>)

    val i = 0
    val x4: (Int) -> Int = bar(when (i) {
                                   10 -> <!UNRESOLVED_REFERENCE!>::baz<!>
                                   20 -> <!UNRESOLVED_REFERENCE!>::baz<!>
                                   else -> <!UNRESOLVED_REFERENCE!>::baz<!>
                               })

    val x5: (Int) -> Int = bar(<!UNRESOLVED_REFERENCE!>::baz<!>!!)

    <!UNRESOLVED_REFERENCE!>(if (true) <!UNRESOLVED_REFERENCE!>::baz<!> else <!UNRESOLVED_REFERENCE!>::baz<!>)(1)<!>
}