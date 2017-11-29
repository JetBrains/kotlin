// !WITH_NEW_INFERENCE
abstract class Runnable {
    abstract fun run()
}

fun foo(): Int {
    val c: Int? = null
    var a: Int?
    if (c is Int) {
        a = 2
        val k = object: Runnable() {
            init {
                a = null
            }
            override fun run() = Unit
        }
        k.run()
        val d: Int = <!DEBUG_INFO_SMARTCAST!>c<!>
        // a is captured so smart cast is not possible
        return d <!NI;NONE_APPLICABLE!>+<!> <!OI;SMARTCAST_IMPOSSIBLE!>a<!>
    }
    else return -1
}
