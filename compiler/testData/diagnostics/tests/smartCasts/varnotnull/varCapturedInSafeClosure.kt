public fun foo() {
    var s: String? = ""
    fun closure(): Int {
        if (s == null) {
            return -1
        } else {
            return 0
        }
    }
    if (s != null) {
        System.out.println(closure())
        // Smart cast is possible, nobody modifies s
        System.out.println(<!DEBUG_INFO_SMARTCAST!>s<!>.length)
    }
}