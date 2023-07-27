import kotlin.contracts.*

class A {
    fun passLambdaValue(l: ContractBuilder.() -> Unit) {
        contract(l)
        <expr>42</expr>
    }
}

fun doSmth(i: String) = 4