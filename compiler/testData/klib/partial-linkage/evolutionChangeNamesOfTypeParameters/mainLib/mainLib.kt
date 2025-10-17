fun lib(): String {
    val a = A<String, String, Int, Int>(19, 17)

    return when {
        a.foo("first") != "first" -> "fail 1"
        a.bar != 19 -> "fail 2"
        a.few != 17 -> "fail 3"
        qux<String, String>("second") != "second" -> "fail 4"

        else -> "OK"
    }
}

