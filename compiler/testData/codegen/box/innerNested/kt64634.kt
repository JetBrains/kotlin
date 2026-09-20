// ISSUE: KT-64634

class Foo(val outer: String) {
    inner class Bar {
        val i: Int
        val nonce: Int

        constructor(i: Int, nonce: Int) {
            this.i = i
            this.nonce = nonce
        }

        constructor(d: Double, nonce: Int) : this(nonce = nonce, i = d.toInt())

        fun result() = outer + i + nonce
    }

    fun createBar() = Bar(0.1, 1)
}

fun box(): String {
    val first = Foo("first").createBar().result()
    if (first != "first01") return "fail: $first"
    val second = Foo("second").createBar().result()
    if (second != "second01") return "fail: $second"
    return "OK"
}
