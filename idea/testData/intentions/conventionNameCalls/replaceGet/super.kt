// IS_APPLICABLE: false

open class Base {
    open operator fun get(s: String) = ""
}

class C : Base() {
    override fun get(s: String): String {
        return super.<caret>get(s)
    }
}
