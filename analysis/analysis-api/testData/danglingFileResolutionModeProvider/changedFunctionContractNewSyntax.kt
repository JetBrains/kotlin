// LANGUAGE: +ContractSyntaxV2
// MODULE: original
import kotlin.contracts.*

fun test1(arg: Any?) contract [
returns() implies (arg != null)
] {
    require(arg != null)
}

// MODULE: copy

fun test1(arg: Any?) contract [
returns() implies (arg == null)
] {
    require(arg != null)
}