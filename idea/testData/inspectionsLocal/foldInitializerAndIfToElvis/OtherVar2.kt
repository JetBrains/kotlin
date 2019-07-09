// PROBLEM: none
class C {
    var x: String? = null

    fun foo(p: List<String?>): Int {
        val v = p[0]
        <caret>if (x == null) return -1
        return v!!.length
    }
}

