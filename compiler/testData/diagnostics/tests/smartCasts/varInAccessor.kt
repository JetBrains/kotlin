var x: Int = 0
    get() {
        var y: Int? = null
        if (y != null) {
            return <!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()
        }
        return field
    }
    set(param) {
        var y: Int? = null
        if (y != null) {
            field = <!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()
        }
        else {
            field = param
        }
    }