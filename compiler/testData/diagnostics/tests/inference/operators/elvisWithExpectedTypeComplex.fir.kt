// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-73031

fun foo(reason: String): String = ""
fun foo(r: Int) {}

fun <R, T> myLet(x: () -> (T) -> R): R? = TODO()

fun lambdaWithExpectedReturnTypeUnit(x: () -> Unit) {}

fun unitFun() {}

fun main() {
    lambdaWithExpectedReturnTypeUnit {
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>myLet<!> {
            ::<!UNRESOLVED_REFERENCE!>foo<!> // UNRESOLVED_REFERENCE in K2, but ok in K1
        } ?: unitFun()
    }
}
