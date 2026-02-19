val x = X()

fun lib(): String {

    val a = qux
    val b = x.bar
    qux = "new global value"
    x.bar = "new member value"

    return when {
        a != "initialized global" -> "fail 1"
        b != "initialized member" -> "fail 2"
        qux != "new global value" -> "fail 3"
        x.bar != "new member value" -> "fail 4"

        else -> "OK"
    }
}

