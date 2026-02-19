// FILE: MyBuilder.kt
class MyBuilder {
    fun contract(contract: String) {
        contract.length
    }

    fun nonContract(contract: String) {
        contract.length
    }
}

// FILE: main.kt
fun test_1() {
    MyBuilder().apply {
        contract("test")
        this@apply.contract("")
    }
}

fun test_2() {
    MyBuilder().apply {
        nonContract("test")
        this@apply.contract("")
    }
}
