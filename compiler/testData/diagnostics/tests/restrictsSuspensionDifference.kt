// FIR_IDENTICAL
// ISSUE: KT-65272
// WITH_STDLIB

@kotlin.coroutines.RestrictsSuspension
object TestScope

val testLambda: suspend TestScope.(Int) -> Unit
    get() = TODO()

suspend fun test() {
    TestScope.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>testLambda<!>(10)
    testLambda(TestScope, 10)
    testLambda.invoke(TestScope, 10)
}
