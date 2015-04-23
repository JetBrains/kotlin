fun foo(/*rename*/aa: Int, b: String) {
}

fun main(args: Array<String>) {
    foo(b = "!", aa = 333)
}
