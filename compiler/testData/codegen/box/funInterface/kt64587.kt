interface Parent {
    fun visit(visitor: (Parent) -> Unit) {
        visitor.invoke(this)
    }
}

fun interface Child : Parent {
    fun intValue(): Int
}

fun box(): String {
    var result = "fail"
    Child { 42 }.visit { result = "OK" }
    return result
}
