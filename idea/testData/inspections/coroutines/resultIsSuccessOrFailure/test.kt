package kotlin
// NO (constructor)
class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}
// YES
fun getSuccess() = success()
// YES
fun getSuccessExplicit(): Result<Int> = Result(456)
// NO (not catching 'correct' available)
fun correctCatching() = Result(true)
// NO (not Result)
fun correct() = true
// YES
fun incorrectCatching() = Result(3.14)
// YES
fun strangeCatching() = runCatching { false }
// NO (not Result)
fun strange() = 1

class Container {
    // YES
    fun classGetSuccess() = Result("123")
    // YES
    fun classGetSuccessExplicit(): Result<Int> = Result(456)
    // NO (not catching 'classCorrect' available)
    fun classCorrectCatching() = Result(true)
    // NO (not Result)
    fun classCorrect() = true
    // YES
    fun classIncorrectCatching() = Result(3.14)
}

fun test() {
    // YES
    fun localGetSuccess() = Result("123")
    // YES
    val anonymous = fun() = Result(45)
    // YES
    val lambda = { Result(true) }
    // NO yet (we do not report local *catching functions)
    fun localCatching() = Result(2.72)
}

// NO (stdlib)
fun success() = Result(true)
// NO (stdlib)
fun failure() = Result(false)
// NO (stdlib)
fun <T> runCatching(block: () -> T) = Result(block())
// NO (stdlib)
fun <T> Result<T>.id() = this

class ClassWithExtension() {
    // NO (not Result)
    fun calc() = 12345
    // NO (not Result)
    fun calcComplex(arg1: Int, arg2: Double): Double = arg1 + arg2
    // YES (different parameters)
    fun calcComplexCatching() = Result(0.0)
}
// NO (extension to calc)
fun ClassWithExtension.calcCatching() = Result(calc())
// NO (not Result)
fun Container.extensionAct() = 42
// YES (different extension receiver)
fun ClassWithExtension.extensionActCatching() = Result(42)