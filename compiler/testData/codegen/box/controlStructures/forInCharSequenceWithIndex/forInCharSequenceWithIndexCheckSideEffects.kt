// WITH_STDLIB

class CountingString(private val s: String) : CharSequence {
    var lengthCtr = 0
    var getCtr = 0

    override val length: Int
        get() = s.length.also { lengthCtr++ }

    override fun subSequence(startIndex: Int, endIndex: Int) = TODO()
    override fun get(index: Int) = s.get(index).also { getCtr++ }
}

val cs = CountingString("abcd")

fun box(): String {
    val s = StringBuilder()

    for ((index, x) in cs.withIndex()) {
        s.append("$index:$x;")
    }

    val ss = s.toString()
    if (ss != "0:a;1:b;2:c;3:d;") return "fail: '$ss'"
    if (cs.lengthCtr != 5) return "lengthCtr != 5, was: '${cs.lengthCtr}'"
    if (cs.getCtr != 4) return "getCtr != 4, was: '${cs.getCtr}'"

    return "OK"
}