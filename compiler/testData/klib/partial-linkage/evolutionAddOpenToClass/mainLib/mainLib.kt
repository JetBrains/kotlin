fun lib(): String = when {
    X().foo() != "in open class" -> "fail 1"
    X().bar != "in open class" -> "fail 2"
    qux().foo() != "in open class" -> "fail 3"
    qux().bar != "in open class" -> "fail 4"

    else -> "OK"
}

