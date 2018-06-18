// WITH_RUNTIME

class Test {
    fun a() {
        <caret>throw myError()
    }
}
fun myError() = RuntimeException()