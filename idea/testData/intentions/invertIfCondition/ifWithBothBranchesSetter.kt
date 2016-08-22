var foo: Boolean = false
    set(arg) {
        <caret>if (field == arg) return
        field = arg
        return
    }