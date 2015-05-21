// "Replace with 'newFun(n + java.math.BigInteger(s))'" "true"

import java.math.BigInteger

@deprecated("", ReplaceWith("newFun(n + java.math.BigInteger(s))", "kotlin.math.plus"))
fun oldFun(n: BigInteger, s: String) {}

fun newFun(n: BigInteger) {}

fun foo() {
    <caret>oldFun(BigInteger("2"), "1")
}
