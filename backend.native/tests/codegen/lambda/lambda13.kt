fun main(args: Array<String>) {
    apply("foo") {
        println(this)
    }
}

fun apply(str: String, block: String.() -> Unit) {
    str.block()
}