// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    @OptIn(ExperimentalContracts::class)
    fun bar(x: () -> Unit) {
        contract {
            callsInPlace(x, InvocationKind.AT_MOST_ONCE)
        }

        if (true) {
            x()
        }
    }

    @OptIn(ExperimentalContracts::class)
    fun foo(x: () -> Unit) {
        contract {
            callsInPlace(x, InvocationKind.AT_LEAST_ONCE)
        }

        x()

        bar {
            x()
        }
    }
}