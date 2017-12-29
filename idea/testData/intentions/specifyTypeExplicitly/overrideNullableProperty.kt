// CHOOSE_NULLABLE_TYPE_IF_EXISTS
// RUNTIME_WITH_FULL_JDK
interface Base {
    val nullable: String?
}

class Tesst : Base {
    override val nullable<caret> = java.lang.String.valueOf("")
}
