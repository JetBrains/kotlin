// LANGUAGE: +ContractSyntaxV2

fun <caret>foo(bool: Boolean) contract [
    returns() implies bool
] = bool
