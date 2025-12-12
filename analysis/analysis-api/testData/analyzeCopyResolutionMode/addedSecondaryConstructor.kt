// MODULE: original
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

class A(val x: Int) {

}

// MODULE: copy
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

class A(val x: Int) {
    @OptIn(ExperimentalContracts::class)
    constructor(block: () -> Unit) : this(5) {
        kotlin.contracts.contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
        block()
        println("hello")
    }
}