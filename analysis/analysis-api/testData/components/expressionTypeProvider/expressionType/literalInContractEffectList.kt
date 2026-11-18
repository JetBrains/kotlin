// LANGUAGE: +ContractSyntaxV2
// WITH_STDLIB
import kotlin.contracts.*

fun f(x: Any?): Boolean contract [returns(<expr>true</expr>) implies (x != null)] {
    return x != null
}
