fun interface SamInterface {
    fun Int.accept(i: String): String
}

val a = SamInterface { a: String ->  "OK" }

fun box(): String {
    with(a) {
        return 1.accept("")
    }
}