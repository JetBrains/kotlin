class My(protected val x: Int) {
    class Her(protected val x: Int)

    inner class Its(protected val x: Int)
}

object Your {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() = 3
}

annotation class His(<!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val x: Int)

enum class Our(protected val x: Int) {
    FIRST(42) {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() = 13
    }
}

interface Their {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() = 7
}