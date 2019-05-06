// "Replace with 'New'" "true"
// WITH_RUNTIME
// ERROR: Type inference failed: Not enough information to infer parameter T in constructor New<T>()<br>Please specify it explicitly.<br>
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

abstract class Main<T>

@Deprecated("", ReplaceWith("New"))
class Old<T, F> : Main<T>()

class New<T> : Main<T>()

fun test() {
    val main = <caret>Old<Int, String>()
}