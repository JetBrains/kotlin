// WITH_RUNTIME

class A {
    var a: List<String>

    init {
        <caret>a = emptyList()
    }
}
