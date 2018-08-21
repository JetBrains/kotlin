package kotlin
// NO (constructor)
class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}
// YES
fun getSuccess() = SuccessOrFailure("123")
// YES
fun getSuccessExplicit(): SuccessOrFailure<Int> = SuccessOrFailure(456)
// NO (noCatching available)
fun correctCatching() = SuccessOrFailure(true)
// NO (not SuccessOrFailure)
fun correct() = true
// YES
fun incorrectCatching() = SuccessOrFailure(3.14)
// YES
fun strangeCatching() = SuccessOrFailure(false)
// YES
fun strange() = 1

class Container {
    // YES
    fun classGetSuccess() = SuccessOrFailure("123")
    // YES
    fun classGetSuccessExplicit(): SuccessOrFailure<Int> = SuccessOrFailure(456)
    // NO (noCatching available)
    fun classCorrectCatching() = SuccessOrFailure(true)
    // NO (not SuccessOrFailure)
    fun classCorrect() = true
    // YES
    fun classIncorrectCatching() = SuccessOrFailure(3.14)
}

fun test() {
    // YES
    fun localGetSuccess() = SuccessOrFailure("123")
    // YES
    val anonymous = fun() = SuccessOrFailure(45)
    // YES
    val lambda = { SuccessOrFailure(true) }
    // NO yet
    fun localCatching() = SuccessOrFailure(2.72)
}