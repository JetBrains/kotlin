// LANGUAGE: +CompanionBlocks +CompanionExtensions
import kotlin.contracts.*

class Owner {
    companion {
        @OptIn(ExperimentalContracts::class)
        fun foo(x: Any?) {
            contract { returns() implies (x is String) }
        }
    }

    fun bar(x: Any?) {
        foo(x)
        <expr>x</expr>
    }
}
