// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    @OptIn(ExperimentalContracts::class)
    fun bar(x: () -> Unit) {
        contract {
            callsInPlace(x, InvocationKind.EXACTLY_ONCE)
        }

        if (true) {
            x.invoke()
            return
        }

        bar(x)
    }

    @OptIn(ExperimentalContracts::class)
    fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
        contract {
            callsInPlace(x, InvocationKind.UNKNOWN)
            callsInPlace(y, InvocationKind.EXACTLY_ONCE)
            callsInPlace(z, InvocationKind.AT_LEAST_ONCE)
        }

        if (true) {
            for (i in 0..0) {
                x.invoke()
            }

            y.invoke()
        } else {
            if (false) {
                y.invoke()
            } else {
                y.invoke()
                z.invoke()
                return
            }
        }

        do {
            bar(z)
        } while (true)
    }
}