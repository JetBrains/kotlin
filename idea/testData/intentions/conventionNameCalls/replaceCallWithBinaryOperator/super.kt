// IS_APPLICABLE: false

open class Base {
    open fun plus(s: String) = ""
}

class C : Base() {
    override fun plus(s: String): String {
        return super.<caret>plus(s)
    }
}
