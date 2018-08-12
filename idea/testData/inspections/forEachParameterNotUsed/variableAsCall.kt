class Invocable {
    operator fun invoke() {}
}

fun test() {
    listOf<Invocable>().forEach {
        it()
    }
}