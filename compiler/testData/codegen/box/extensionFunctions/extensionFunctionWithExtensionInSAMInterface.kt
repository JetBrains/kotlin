var result = ""

fun interface SamInterface {
    fun (Int.()->String).accept(): String
}

fun (Int.()-> String).foo(a: Int.()-> String): String { return "OK" }
fun <T> test(): Int.() -> T {
    return { "" as T }
}

val a = SamInterface { test<String>().foo(test<String>()) }

fun box(): String {
    with(a) {
        result += test<String>().accept()
    }
    return result
}