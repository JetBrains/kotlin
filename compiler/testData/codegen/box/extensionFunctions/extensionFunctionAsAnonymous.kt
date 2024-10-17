val a = fun String.(y: String): String { return this + y }

fun box(): String {
    return if (a("O", "K") == "O".a("K")) "OK" else "FAIL"
}