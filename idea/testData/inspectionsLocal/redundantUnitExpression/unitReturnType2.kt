// WITH_RUNTIME

fun <T> doIt(p: () -> T): T = TODO()

fun g(p: String?) {

    p?.let { Unit }
}

fun h() = Unit

fun x() = doIt { Unit<caret> }
