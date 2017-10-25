// WITH_RUNTIME
fun test() {
    when<caret> {
        true -> {
            println(1)
        }
        else -> {
            println(2)
        }
    }
}