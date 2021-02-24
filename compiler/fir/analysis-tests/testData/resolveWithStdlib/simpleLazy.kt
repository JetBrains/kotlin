//val x = lazy { "Hello" }.getValue(null, throw null)
val x by lazy { "Hello" }

fun foo() {
    x.length

    val y by lazy { "Bye" }
    y.length
}

class Some {
    val z by lazy { "Some" }

    fun foo() {
        z.length
    }
}