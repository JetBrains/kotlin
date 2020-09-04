import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

fun <T> identity(any: T): T = any

fun length(x: String?): Any? {
    contract {
        (x != null || x is String) implies (returnValue() is String)
    }
    return x?.plus("A")
}

fun test1(x: String) {
    val i = length(x)

    val asString = i.length
    if (length(x).get(0) == 'A') println("A")
}

fun test2(x: String?) {
    val i = length(x)

    val asString = i.<!UNRESOLVED_REFERENCE!>length<!>
    if (length(x).<!UNRESOLVED_REFERENCE!>get<!>(0) == 'A') println("A")
}

fun test3(x: String) {
    val i = identity(length(x))

    val asString = i.length
    if (identity(length(x)).get(0) == 'A') println("A")
}