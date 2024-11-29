fun wrapper(block: () -> Unit) { block() }

private lateinit var o: String

class A {
    internal inline fun inlineMethod(): String {
        lateinit var k: String
        wrapper {
            o = "O"
            k = "K"
        }
        return o + k
    }
}

fun box() = A().inlineMethod()
