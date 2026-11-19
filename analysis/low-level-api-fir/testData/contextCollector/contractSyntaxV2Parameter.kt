// LANGUAGE: +ContractSyntaxV2
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo(x: Any?) contract [returns() implies (<expr>x</expr> is String)] {}
