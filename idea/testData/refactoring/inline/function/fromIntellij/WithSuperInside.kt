class Usage {
    fun foo() {
        <caret>bar()
    }

    fun bar() {
        object : W() {
            override fun www() {
                super.www()
            }
        }
    }
}

class W {
    protected fun www() {}
}