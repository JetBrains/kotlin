// CHOOSE_NULLABLE_TYPE_IF_EXISTS
// RUNTIME_WITH_FULL_JDK
interface Base {
    fun notNullFun(): String
}

class Tesst : Base {
    override fun notNullFun()<caret> = java.lang.String.valueOf("")
}
