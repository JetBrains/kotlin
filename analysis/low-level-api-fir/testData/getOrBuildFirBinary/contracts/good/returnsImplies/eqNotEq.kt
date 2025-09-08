// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    @OptIn(ExperimentalContracts::class)
    fun checkNotNull(x: Any?): Boolean {
        contract {
            returns(true) implies (x != null)
            returns(false) implies (x == null)
        }
        return x != null
    }

    @OptIn(ExperimentalContracts::class)
    fun trickyRequireNotNull(x: Any?) {
        contract {
            returns() implies (!(x == null))
        }
        if (x == null) {
            throw IllegalStateException()
        }
    }
}