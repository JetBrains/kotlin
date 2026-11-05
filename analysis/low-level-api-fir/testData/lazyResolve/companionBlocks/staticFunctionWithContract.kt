// LANGUAGE: +CompanionBlocks +CompanionExtensions
import kotlin.contracts.*

class Foo {
    companion {
        @OptIn(ExperimentalContracts::class)
        fun staticFun<caret>ction(x: Any?) {
            contract { returns() implies (x is String) }
        }
    }
}
