fun foo() = "OK"

fun box(): String {
    val x = ::foo

    var r = x()
    if (r != "OK") return r

    r = run(::foo)
    if (r != "OK") return r

    return "OK"
}
