import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

interface A {
    fun foo() {}
}

interface B {
    fun bar() {}
}

class AB : A, B

fun Any.asAB(): Any {
    contract {
        (this@asAB is AB) implies (returnValue() is A && returnValue() is B)
    }
    return this
}

fun Any.otherAsAB(): Any {
    contract {
        (this@otherAsAB is AB) implies (returnValue() is A)
        (this@otherAsAB is AB) implies (returnValue() is B)
    }
    return this
}

fun test(value: AB) {
    val ab = value.otherAsAB()
    ab.foo() // OK
    ab.bar() // OK
}