trait X {
    open fun fn() {}
}

open class Y {
    public open fun fn() {}
}

class Z : Y(), X {
    override fun fn() {
        super<<caret>>.fn()
    }
}