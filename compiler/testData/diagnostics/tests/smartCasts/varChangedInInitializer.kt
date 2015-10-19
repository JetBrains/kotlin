class My {
    init {
        var y: Int?
        y = 42
        <!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()
    }
}