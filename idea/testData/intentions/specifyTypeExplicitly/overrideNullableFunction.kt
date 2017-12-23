// CHOOSE_NULLABLE_TYPE_IF_EXISTS
// RUNTIME_WITH_FULL_JDK
interface Base {
    fun nullableFun(): String?
}

class Tesst : Base {
    override fun nullableFun()<caret> = java.lang.String.valueOf("")
}
