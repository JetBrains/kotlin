// INTERRUPT_AT: 0

// The intent of the test is to correctly resolve the initializer first on the implicit type phase
// and then resolve the getter on the body phase.
// KT-75858 shows that this might lead to the initializer disappearing
val foo = 1
    get() {
        val t: broken.lib.Foo? = null
        t?.result
        <expr>field</expr>
        return 0
    }
