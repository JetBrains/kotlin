package kotlin
// NO (constructor)
class SuccessOrFailure<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}
// YES
fun getSuccess() = success()
// YES
fun getSuccessExplicit(): SuccessOrFailure<Int> = SuccessOrFailure(456)
// NO (not catching 'correct' available)
fun correctCatching() = SuccessOrFailure(true)
// NO (not SuccessOrFailure)
fun correct() = true
// YES
fun incorrectCatching() = SuccessOrFailure(3.14)
// YES
fun strangeCatching() = runCatching { false }
// NO (not SuccessOrFailure)
fun strange() = 1

class Container {
    // YES
    fun classGetSuccess() = SuccessOrFailure("123")
    // YES
    fun classGetSuccessExplicit(): SuccessOrFailure<Int> = SuccessOrFailure(456)
    // NO (not catching 'classCorrect' available)
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
    // NO yet (we do not report local *catching functions)
    fun localCatching() = SuccessOrFailure(2.72)
}

// NO (stdlib)
fun success() = SuccessOrFailure(true)
// NO (stdlib)
fun failure() = SuccessOrFailure(false)
// NO (stdlib)
fun <T> runCatching(block: () -> T) = SuccessOrFailure(block())
// NO (stdlib)
fun <T> SuccessOrFailure<T>.id() = this

class ClassWithExtension() {
    // NO (not SuccessOrFailure)
    fun calc() = 12345
    // NO (not SuccessOrFailure)
    fun calcComplex(arg1: Int, arg2: Double): Double = arg1 + arg2
    // YES (different parameters)
    fun calcComplexCatching() = SuccessOrFailure(0.0)
}
// NO (extension to calc)
fun ClassWithExtension.calcCatching() = SuccessOrFailure(calc())
// NO (not SuccessOrFailure)
fun Container.extensionAct() = 42
// YES (different extension receiver)
fun ClassWithExtension.extensionActCatching() = SuccessOrFailure(42)