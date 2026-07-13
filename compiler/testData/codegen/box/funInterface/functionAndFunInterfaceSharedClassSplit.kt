fun interface Parser {
    fun parse(s: String): Int
}

fun len(s: String): Int = s.length

fun box(): String {
    val f: (String) -> Int = ::len
    val p = Parser(::len)

    if (f("OK") != 2) return "FAIL f: ${f("OK")}"
    if (p.parse("OK") != 2) return "FAIL p: ${p.parse("OK")}"

    return "OK"
}
