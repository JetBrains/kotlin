// IS_APPLICABLE: false

open class Base {
    open fun minus() = this
}

class C : Base() {
    override fun minus(): Base {
        return super.<caret>minus()
    }
}
