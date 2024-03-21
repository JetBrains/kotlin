// ISSUE: KT-65272
// WITH_STDLIB

@kotlin.coroutines.RestrictsSuspension
object TestScope

val testLambda: suspend TestScope.() -> Unit
    get() = TODO()

suspend fun test() {
    TestScope.<!ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL!>testLambda<!>()
    testLambda(TestScope)
    testLambda.invoke(TestScope)
}
