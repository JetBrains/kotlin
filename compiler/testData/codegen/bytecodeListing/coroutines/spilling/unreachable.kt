// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

fun use(c: suspend (String) -> Unit) {}

fun test() {
    use {
        throw IllegalStateException("")
        it + ""
    }
}