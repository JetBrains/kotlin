// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
// LANGUAGE: +ContractSyntaxV2
import kotlin.contracts.*

class Foo{
    fun test1(arg: Any?) contract [
        returns() implies (arg != null)
    ] {
        require(arg != null)
    }

    fun test2(s: String?, block: () -> Unit) contract [
        returns() implies (s != null),
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    ] {
        require(s != null)
        block()
    }

    fun test3(arg: Any?): Boolean contract [
        returns(true) implies (arg != null)
    ] {
        require(arg != null)
        return true
    }
}