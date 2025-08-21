// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    @OptIn(ExperimentalContracts::class)
    inline fun inlineRun(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        block()
    }

    @OptIn(ExperimentalContracts::class)
    fun myRun(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.UNKNOWN)
        }
        block()
    }
}