// RUN_PIPELINE_TILL: FRONTEND
// JAVAC_EXPECTED_FILE
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

fun <T : Throwable> tryCatch() {
    try { } catch (<!TYPE_PARAMETER_IN_CATCH_CLAUSE!>e: T<!>) { }
}

fun <T : Nothing?> test1() {
    try {
        throw Exception()
    } catch (<!TYPE_PARAMETER_IN_CATCH_CLAUSE!>x: T & Any<!>) {
    }
}