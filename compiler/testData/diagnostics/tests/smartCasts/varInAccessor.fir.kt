var x: Int = 0
    get() {
        var y: Int? = null
        if (y != null) {
            return y.hashCode()
        }
        return field
    }
    set(param) {
        var y: Int? = null
        if (y != null) {
            field = y.hashCode()
        }
        else {
            field = param
        }
    }