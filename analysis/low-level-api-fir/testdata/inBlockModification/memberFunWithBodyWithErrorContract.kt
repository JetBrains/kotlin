import kotlin.contracts.*

class A {
    fun passLa<caret>mbdaValue(l: ContractBuilder.() -> Unit) {
        contract(l)
    }
}

fun doSmth(i: String) = 4