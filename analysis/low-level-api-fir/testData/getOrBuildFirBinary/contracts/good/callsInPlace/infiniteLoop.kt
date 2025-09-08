// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    fun getCondition(): Boolean = true

    @OptIn(ExperimentalContracts::class)
    fun test_2(f: () -> Unit) {
        contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }
        do {
            f()
        } while (true)
    }

    @OptIn(ExperimentalContracts::class)
    fun test_4(f: () -> Unit) {
        contract { callsInPlace(f, InvocationKind.AT_LEAST_ONCE) }
        do {
            f()
        } while (getCondition())
    }
}