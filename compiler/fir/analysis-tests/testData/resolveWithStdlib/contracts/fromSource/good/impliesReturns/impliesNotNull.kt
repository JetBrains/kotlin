import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

open class ValueReference
class ValueReferenceImpl(val value: String) : ValueReference()

fun ValueReference.value(): String? {
    contract {
        (this@value is ValueReferenceImpl) implies (returnValue() != null)
    }
    return (this as? ValueReferenceImpl)?.value
}

fun test(ref: ValueReferenceImpl) {
    val value = ref.value()
    val valueLength = value.length // OK
}