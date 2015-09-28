class My {

    val y: Int 
        get() {
            var x: Int?
            x = 3
            return <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
        }

    fun test() {
        var x: Int?
        x = 2
        <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
        fun bb() {
           var <!NAME_SHADOWING!>x<!>: Any?
           x = 4
           <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
        x = 4
        // Really smart cast is possible but name shadowing by bb() prevents it
        <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
    }
}