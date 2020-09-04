import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

open class Container
class NullContainer : Container()
class NotNullContainer(val value: Any) : Container() {
    fun foo() {}
}

fun Any?.toContainer(): Container {
    contract {
        (this@toContainer != null) implies (returnValue() is NotNullContainer)
    }
    return if (this != null) NotNullContainer(this) else NullContainer()
}

fun test(value: String) {
    val value = value.toContainer()
    value.foo() // OK
}