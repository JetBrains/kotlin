// FIR_IDENTICAL
fun use(fn: (Int) -> Unit) { fn(1) }

class Host {
    fun withVararg(vararg xs: Int) = ""

    fun testImplicitThis() {
        use(::withVararg)
    }

    fun testBoundReceiverLocalVal() {
        val h = Host()
        use(h::withVararg)
    }

    fun testBoundReceiverLocalVar() {
        var h = Host()
        use(h::withVararg)
    }

    fun testBoundReceiverParameter(h: Host) {
        use(h::withVararg)
    }

    fun testBoundReceiverExpression() {
        use(Host()::withVararg)
    }
}
