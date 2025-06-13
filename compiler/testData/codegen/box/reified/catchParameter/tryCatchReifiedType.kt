// LANGUAGE: +AllowReifiedTypeInCatchClause
// IGNORE_BACKEND_K1: ANY

inline fun <reified E : Throwable> catch(block: () -> Nothing) {
    try {
        block()
    } catch (ignore: E) {
    } finally {
    }
}

inline fun <reified T : Throwable> evalCatch(block: () -> Nothing): String {
    return try {
        catch<T>(block)

        "Y"
    } catch (throwable: Throwable) {
        "N"
    } finally {
    }
}

fun box(): String {
    var log = ""

    log += evalCatch<Throwable> { throw Throwable() }
    log += evalCatch<Throwable> { throw Exception() }
    log += evalCatch<Throwable> { throw IllegalStateException() }
    log += evalCatch<Throwable> { throw IllegalArgumentException() }

    log += " "

    log += evalCatch<Exception> { throw Throwable() }
    log += evalCatch<Exception> { throw Exception() }
    log += evalCatch<Exception> { throw IllegalStateException() }
    log += evalCatch<Exception> { throw IllegalArgumentException() }

    log += " "

    log += evalCatch<IllegalStateException> { throw Throwable() }
    log += evalCatch<IllegalStateException> { throw Exception() }
    log += evalCatch<IllegalStateException> { throw IllegalStateException() }
    log += evalCatch<IllegalStateException> { throw IllegalArgumentException() }

    log += " "

    log += evalCatch<IllegalArgumentException> { throw Throwable() }
    log += evalCatch<IllegalArgumentException> { throw Exception() }
    log += evalCatch<IllegalArgumentException> { throw IllegalStateException() }
    log += evalCatch<IllegalArgumentException> { throw IllegalArgumentException() }

    log += " "

    log += evalCatch<NoSuchElementException> { throw Throwable() }
    log += evalCatch<NoSuchElementException> { throw Exception() }
    log += evalCatch<NoSuchElementException> { throw IllegalStateException() }
    log += evalCatch<NoSuchElementException> { throw IllegalArgumentException() }

    if (log != "YYYY NYYY NNYN NNNY NNNN") return "Fail: $log"

    return "OK"
}
