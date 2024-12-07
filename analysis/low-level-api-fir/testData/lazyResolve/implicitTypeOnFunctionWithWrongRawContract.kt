// LANGUAGE: +ContractSyntaxV2
import kotlin.contracts.*

fun <caret>foo(bool: Boolean) contract [
    abc
] = bool
