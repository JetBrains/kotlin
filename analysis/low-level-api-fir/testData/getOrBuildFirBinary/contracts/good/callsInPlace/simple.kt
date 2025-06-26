// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    @OptIn(ExperimentalContracts::class)
    fun bar(x: () -> Unit) {
        contract {
            callsInPlace(x, InvocationKind.EXACTLY_ONCE)
        }

        x.invoke()
    }

    @OptIn(ExperimentalContracts::class)
    fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
        contract {
            callsInPlace(x, InvocationKind.EXACTLY_ONCE)
            callsInPlace(y, InvocationKind.AT_MOST_ONCE)
            callsInPlace(z, InvocationKind.EXACTLY_ONCE)
        }

        x.invoke()

        if (true) {
            y()
        }

        bar(z)
    }
}