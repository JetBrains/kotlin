// See KT-20104

interface Logger

fun logger(f: () -> Logger): Logger = object : Logger {}

val logger: Logger get() = logger(::logger)