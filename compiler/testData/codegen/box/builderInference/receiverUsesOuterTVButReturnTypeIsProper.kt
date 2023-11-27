class Buildee<CT> {
    var r: CT? = null
    fun yield(arg: CT) {
        r = arg
    }
}

fun <FT> build(instructions: Buildee<FT>.() -> Unit): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}


class Logger {
    fun error() {
        throw RuntimeException("")
    }
}

fun <T> logger(arg: T): Logger = Logger()

fun box(): String {
    val b = build {
        if ("".hashCode() == 0) {
            yield("OK")
        } else {
            logger(this).error()
        }
    }
    return b.r ?: "fail"
}