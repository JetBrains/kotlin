// CHOOSE_NULLABLE_TYPE_IF_EXISTS
// WITH_RUNTIME
interface Base {
    fun nullableFun(): String?
}

class Tesst : Base {
    override fun nullableFun()<caret> = java.lang.String.valueOf("")
}
