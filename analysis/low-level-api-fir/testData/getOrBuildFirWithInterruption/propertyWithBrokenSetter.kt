// INTERRUPT_AT: 0

// The intent of the test is to correctly resolve the getter first on the implicit type phase
// and then resolve the setter on the body phase
var foo
    get() = 1
    set(value) {
        val brokenProperty: broken.lib.Foo? = null
        <expr>brokenProperty?.result</expr>
    }
