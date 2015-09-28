fun foo() {
    class My {
        val x: Int
        init {
            var y: Int?
            y = 42
            x = <!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()
        }
    }
}