// FIR_IDENTICAL

fun box(b: Boolean) {
    generate {
        expectIntf(object : Intf {
            override fun foo() {
                if (b) {
                    // Do not debounce regex toggle event.
                    someUnit()
                }
                else {
                    yield(Unit)
                }
            }
        })

        someUnit()
    }

}

fun expectIntf(intf: Intf) {}

fun someUnit() {}

interface Intf {
    fun foo()
}

interface Controller<F> {
    fun yield(t: F)
}

fun <S> generate(g: suspend Controller<S>.() -> Unit) {}