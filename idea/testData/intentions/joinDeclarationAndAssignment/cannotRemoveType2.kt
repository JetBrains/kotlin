// WITH_RUNTIME

class A {
    var a<caret>: List<String>

    init {
        a = emptyList()
    }
}
