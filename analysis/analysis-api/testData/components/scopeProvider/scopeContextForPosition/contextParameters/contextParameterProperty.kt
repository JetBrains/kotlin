context(c: Boolean)
val foo: Boolean
    get() {
        return <expr>c</expr>
    }

// LANGUAGE: +ContextParameters