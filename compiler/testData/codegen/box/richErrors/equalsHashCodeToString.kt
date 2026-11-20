// LANGUAGE: +RichErrors
error class Foo(val x: String)

fun box(): String {
    val a = Foo("OK")
    if (a != Foo("OK")) return "FAIL equals"
    if (a.hashCode() != 2524) return "FAIL hashCode: ${a.hashCode()}"
    if (a.toString() != "Foo(x=OK)") return "FAIL toString: ${a.toString()}"

    return "OK"
}
