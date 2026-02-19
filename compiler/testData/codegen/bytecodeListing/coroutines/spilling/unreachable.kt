// WITH_STDLIB

fun use(c: suspend (String) -> Unit) {}

fun test() {
    use {
        throw IllegalStateException("")
        it + ""
    }
}
