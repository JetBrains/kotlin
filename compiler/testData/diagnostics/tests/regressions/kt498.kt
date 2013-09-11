// KT-498 Very strange error in the type checker

class IdUnavailableException() : Exception() {}

fun <T : Any> T.getJavaClass() : Class<T> {
    return <!UNCHECKED_CAST!>((this as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>).getClass()) as Class<T><!> // Some error here, because of Exception() used above. ?!!!
}