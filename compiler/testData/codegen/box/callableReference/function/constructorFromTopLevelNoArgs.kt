class A {
    var result = "OK"
}

fun box() = (::A).let { it() }.result
