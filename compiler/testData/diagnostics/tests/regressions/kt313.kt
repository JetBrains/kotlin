// KT-313 Bug in substitutions in a function returning its type parameter T

fun <T> Iterable<T>.join(separator : String?) : String {
    return separator.npe()
}

fun <T : Any> T?.npe() : T {
    if (this == null)
      throw NullPointerException()
    return <!DEBUG_INFO_SMARTCAST!>this<!>;
}
