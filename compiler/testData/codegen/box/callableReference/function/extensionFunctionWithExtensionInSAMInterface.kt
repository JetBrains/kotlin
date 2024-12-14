var result = ""

fun interface SamInterface {
    fun (Int.()->String).accept(): String
}

fun (Int.()-> String).foo(a: Int.()-> String): String { return "OK" }

fun <T> test(): Int.() -> T {
    return { "" as T }
}

val b = SamInterface (test<String>()::foo)

fun box(): String {
    with(b) {
        result += test<String>().accept()
    }
    return result
}

