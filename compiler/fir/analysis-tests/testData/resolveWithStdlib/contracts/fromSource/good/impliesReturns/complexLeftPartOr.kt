import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

fun Any.foo() = 1

fun Any?.orElse(other: Any?): Any? {
    contract {
        (this@orElse != null || other != null) implies (returnValue() != null)
    }
    return (this ?: other)
}

fun test1(x: String?, y: String) {
    val z = x.orElse(y)
    z.foo() // OK
}

fun test2(x: String, y: String?) {
    val z = x.orElse(y)
    z.foo() // OK
}

fun test3(x: String, y: String) {
    val z = x.orElse(y)
    z.foo() // OK
}

fun test3(x: String?, y: String?) {
    val z = x.orElse(y)
    z.<!INAPPLICABLE_CANDIDATE!>foo<!>() // NOT OK
}