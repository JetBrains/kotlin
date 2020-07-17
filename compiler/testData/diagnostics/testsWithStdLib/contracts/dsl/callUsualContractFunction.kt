// !DIAGNOSTICS: -UNUSED_PARAMETER

package my

// Accepts block, can't be distinguished from kotlin.contract without resolve
fun contract(block: () -> Unit) {}

// Accepts int, potentially can be distinguished from kotlin.contract by PSI,
// but as of Kotlin 1.3.0, we don't do that
fun contract(i: Int) {}

fun doStuff() {}

class SomeClass {

    fun contract() {}

    fun callMemberContractWithThis() {
        this.contract()
    }

    fun callMemberContractWithoutThis() {
        contract()
    }

    fun callTopLevelSamePsiInMember() {
        contract { }
    }
}

fun callTopLevelSamePsi() {
    contract { }
}

fun callTopLevelDifferentPsi() {
    contract(42)
}

fun callTopLevelSamePsiNotFirstStatement() {
    doStuff()
    contract { }
}
