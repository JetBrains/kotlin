// LANGUAGE: +ContractSyntaxV2
import kotlin.contracts.*

fun calculateNumber(block: () -> Int): Int contract [
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
] = block()

fun <R> calculateResult(num: Int?, calculate: (Int?) -> R): R contract [
    callsInPlace(calculate, InvocationKind.EXACTLY_ONCE),
    returns() implies (num != null)
] = calculate(num)
