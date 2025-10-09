fun lib(): String {
    val x = X()
    foo(x)

    return when {
        bar != "changed global value" -> "fail 1"
        x.qux != "changed member value" -> "fail 2"

        else -> "OK"
    }
}

