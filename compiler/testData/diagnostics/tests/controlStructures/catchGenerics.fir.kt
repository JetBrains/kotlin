// JAVAC_EXPECTED_FILE
// See KT-9816, KT-9742

// Not allowed in Java
class ZException<T>(val p: T) : Exception()

class YException<T>(val p: T): java.lang.RuntimeException()

class XException<T>(val p: T): Throwable()

fun bar() {
    try {
        throw ZException(11)
    } catch (e: ZException<*>) {}    
}

inline fun <reified E : Exception, R> tryCatch(lazy: () -> R, failure: (E) -> R): R =
    try {
        lazy()
    } catch (e: E) {
        failure(e)
    }

fun <T : Throwable> tryCatch() {
    try { } catch (e: T) { }
}