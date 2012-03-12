// KT-498 Very strange error in the type checker

class IdUnavailableException() : Exception() {}

fun <T : Any> T.getJavaClass() : Class<T> {
    return <!UNCHECKED_CAST!>((this <!CAST_NEVER_SUCCEEDS!>as<!> Object).getClass()) as Class<T><!> // Some error here, because of Exception() used above. ?!!!
}
