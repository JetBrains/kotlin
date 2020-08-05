import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

fun <T> identity(any: T): T = any

val something: Any?
    get() {
        contract {
            always() implies (returnValue() is String)
        }
        return "null"
    }

fun test1() {
    val i = something

    val asString = i.length
    if (something.get(0) == 'A') println("A")
}

fun test2() {
    val i = identity(something)

    val asString = i.length
    if (identity(something).get(0) == 'A') println("A")
}