import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

fun <K> id(x: K): K = x

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
    val i = id(something)

    val asString = i.length
    if (id(something).get(0) == 'A') println("A")
}