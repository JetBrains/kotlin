fun interface SamInterface {
    fun Int.accept(): String
}

val Int.a : String
    get() = "OK"

val b = SamInterface { a }

fun box(): String {
    with(b) {
        return 1.accept()
    }
}