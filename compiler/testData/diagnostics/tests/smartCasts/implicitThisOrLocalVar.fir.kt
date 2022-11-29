class Box(var item: String?)

fun expectString(it: String) {}

fun Box.test() {
    val other = Box("")
    myRun {
        if (item != null) {
            expectString(<!SMARTCAST_IMPOSSIBLE!>item<!>)
            other.item = null
            expectString(<!SMARTCAST_IMPOSSIBLE!>item<!>)
            this.item = null
            expectString(<!ARGUMENT_TYPE_MISMATCH!>item<!>)
        }
    }

    var item: String? = "OK"
    myRun {
        if (item != null) {
            this.item = null
            expectString(item)
        }
    }
}

fun myRun(block: () -> Unit) {}