// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73031

fun foo(reason: String): String = ""
fun foo(r: Int) {}

fun <R, T> myLet(x: () -> (T) -> R): R? = TODO()

fun lambdaWithExpectedReturnTypeUnit(x: () -> Unit) {}

fun unitFun() {}

fun main() {
    lambdaWithExpectedReturnTypeUnit {
        myLet {
            ::foo // UNRESOLVED_REFERENCE in K2, but ok in K1
        } ?: unitFun()
    }
}
