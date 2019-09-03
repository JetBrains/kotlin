//val x = lazy { "Hello" }.getValue(null, throw null)
val x by lazy { "Hello" }

fun foo() {
    x.length

    val y by lazy { "Bye" }
    y.length
}