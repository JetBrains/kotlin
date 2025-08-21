// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
class Unmarked {
    fun getStuff(): String = ""

    var prop: String = ""
        get() = field + ""
        set(value) {
            field = value
        }

    class Marked {
        fun alreadyApplied(): String = ""

        var prop: String = ""
            get() = field + ""
            set(value) {
                field = value
            }
    }

    enum class E {
        A, B;
        fun foo() = ""
    }
}
