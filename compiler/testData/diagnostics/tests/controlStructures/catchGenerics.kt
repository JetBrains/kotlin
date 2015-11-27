// See KT-9816, KT-9742

// Not allowed in Java
open class ZException<T>(val p: T) : Exception() 

fun foo(): String {
    try {
        throw ZException(11)
    } catch (e: ZException<String>) {
        return e.p
    }
}

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
