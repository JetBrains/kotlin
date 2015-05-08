// "Change 'A.plus' function return type to '() -> Int'" "true"
trait A {
    fun plus(a: A): String
}

fun foo(a: A): () -> Int {
    return a + a<caret>
}