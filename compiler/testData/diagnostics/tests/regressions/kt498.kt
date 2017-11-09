// JAVAC_EXPECTED_FILE
// KT-498 Very strange error in the type checker

class IdUnavailableException() : Exception() {}

fun <T : Any> T.getJavaClass() : Class<T> {
    return ((this as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>).getClass()) <!UNCHECKED_CAST!>as Class<T><!> // Some error here, because of Exception() used above. ?!!!
}