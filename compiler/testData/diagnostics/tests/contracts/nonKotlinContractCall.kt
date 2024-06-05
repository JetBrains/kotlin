// FIR_IDENTICAL
// ISSUE: KT-68820

class MyBuilder {
    fun contract(contract: String) {
        contract.length
    }

    fun nonContract(contract: String) {
        contract.length
    }
}

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
