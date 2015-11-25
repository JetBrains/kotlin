fun String?.foo() = this ?: "OK"

fun foo(i: Int?): String {
    if (i == null) return i.foo()
    return "$i"
}

fun box() = foo(null)
