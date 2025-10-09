class Z: X() 

fun lib(): String = when {
    X().foo() != "open method" -> "fail 1"
    X().bar != "open property" -> "fail 2"
    Y().foo() != "derived method" -> "fail 3"
    Y().bar != "derived property" -> "fail 4"
    Z().foo() != "open method" -> "fail 5"
    Z().bar != "open property" -> "fail 6"

    else -> "OK"
}

