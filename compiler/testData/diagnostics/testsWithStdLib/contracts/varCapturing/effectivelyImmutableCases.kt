// FIR_IDENTICAL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MutablePerson(var name: String = "NoName", val age: String = "1", var child: MutablePerson = MutablePerson())

fun barRegular(f: () -> Unit) {
    println("Inside barRegular")
    f()
}

fun baz(s: String) {
    println("baz called with: $s")
}

private fun testStable() = barRegular {

    var person2 = MutablePerson("Alice")

    barRegular {
        baz(person2.age) // should be warning or not?
    }
}
