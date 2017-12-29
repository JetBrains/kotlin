// CHOOSE_NULLABLE_TYPE_IF_EXISTS
// RUNTIME_WITH_FULL_JDK
interface Base {
    val notNull: String
}

class Tesst : Base {
    override val notNull<caret> = java.lang.String.valueOf("")
}
