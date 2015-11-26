// IS_APPLICABLE: false

open class Base {
    open operator fun unaryMinus() = this
}

class C : Base() {
    override fun unaryMinus(): Base {
        return super.<caret>unaryMinus()
    }
}
