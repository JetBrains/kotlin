class Y: X() 

fun lib(): String = when {
    Y().foo() != "in non-abstract class" -> "fail 1"
    Y().bar != "in non-abstract class" -> "fail 2"

    else -> "OK"
}

