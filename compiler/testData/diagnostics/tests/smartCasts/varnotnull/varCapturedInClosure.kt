public fun foo() {
    var s: String? = ""
    fun closure(): Int {
        if (s == "") {
            s = null
            return -1
        } else if (s == null) {
            return -2
        } else {
            return <!SMARTCAST_IMPOSSIBLE!>s<!>.length // Here smartcast is possible, at least in principle
        }
    }
    if (s != null) {
        System.out.println(closure())
        System.out.println(<!SMARTCAST_IMPOSSIBLE!>s<!>.length)   // Here smartcast is not possible due to a closure predecessor
    }
}