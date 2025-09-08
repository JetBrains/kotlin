// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
import kotlin.contracts.*

class Foo {
    @OptIn(ExperimentalContracts::class)
    fun test1(x: String?): Int? {
        contract {
            returnsNotNull() implies (x != null)
        }

        return x?.length
    }

    @OptIn(ExperimentalContracts::class)
    fun test2(x: String?): Int? {
        contract {
            returnsNotNull() implies (x is String)
        }

        return x?.length
    }
}