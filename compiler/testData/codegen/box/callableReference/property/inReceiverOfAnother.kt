var x = "OK"

class C(init: () -> String) {
    val value = init()
}

fun box() = C(::x)::value.get()
