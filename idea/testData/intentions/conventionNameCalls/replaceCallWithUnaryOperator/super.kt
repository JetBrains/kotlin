// IS_APPLICABLE: false

open class Base {
    open fun unaryMinus() = this
}

class C : Base() {
    override fun unaryMinus(): Base {
        return super.<caret>unaryMinus()
    }
}
