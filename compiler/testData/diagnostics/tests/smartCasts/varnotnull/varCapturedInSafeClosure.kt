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
        // Smart cast is possible but closure makes it harder to understand
        System.out.println(s<!UNSAFE_CALL!>.<!>length())   
    }
}