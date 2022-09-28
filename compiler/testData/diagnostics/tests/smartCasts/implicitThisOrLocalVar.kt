class Box(var item: String?)

fun <T> take(it: T) {}

fun Box.test() {
    val other = Box("")
    myRun {
        if (item != null) {
            take<String>(<!SMARTCAST_IMPOSSIBLE!>item<!>)
            other.item = null
            take<String>(<!SMARTCAST_IMPOSSIBLE!>item<!>)
            this.item = null
            take<String>(<!TYPE_MISMATCH!>item<!>)
        }
    }

    var item: String? = "OK"
    myRun {
        if (item != null) {
            this.item = null
            take<String>(<!DEBUG_INFO_SMARTCAST!>item<!>)
        }
    }
}

fun myRun(block: () -> Unit) {}