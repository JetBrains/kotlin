fun lib(): String {
    val x = X("first", "second")
    return when {
        x.a != "first" -> "fail 1"
        x.b != "second" -> "fail 2"
        x.foo() != "firstsecond" -> "fail 3"
        x.bar != "secondfirst" -> "fail 4"
        x.toString() != "X(a=first, b=second)" -> "fail 5"
        else -> "OK"
    }
}

