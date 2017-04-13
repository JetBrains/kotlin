// See KT-9816, KT-9742

// Not allowed in Java
class ZException<!GENERIC_THROWABLE_SUBCLASS!><T><!>(val p: T) : Exception()

class YException<!GENERIC_THROWABLE_SUBCLASS!><T><!>(val p: T): java.lang.RuntimeException()

class XException<!GENERIC_THROWABLE_SUBCLASS!><T><!>(val p: T): Throwable()

fun bar() {
    try {
        throw ZException(11)
    } catch (e: ZException<*>) {}    
}

inline fun <reified E : Exception, R> tryCatch(lazy: () -> R, failure: (E) -> R): R =
    try {
        lazy()
    } catch (<!REIFIED_TYPE_IN_CATCH_CLAUSE!>e: E<!>) {
        failure(e)
    }

fun <T : Throwable> tryCatch() {
    try { } catch (<!TYPE_PARAMETER_IN_CATCH_CLAUSE!>e: T<!>) { }
}