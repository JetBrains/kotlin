var myProperty = listOf(1, 2, 3)
    get() {
        return field + field
    }
    set(param) {
        field = param
    }