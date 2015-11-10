val x: Int? = 0
    get() {
        if (field != null) return <!DEBUG_INFO_SMARTCAST!>field<!>.hashCode()
        return null
    }
