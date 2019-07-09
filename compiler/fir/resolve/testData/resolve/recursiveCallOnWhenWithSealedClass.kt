// Issue: KT-18583

sealed class Maybe<T> {
    class Nope<T>(val reasonForLog: String, val reasonForUI: String) : Maybe<T>()
    class Yeah<T>(val meat: T) : Maybe<T>()

    fun unwrap() = when (this) {
        is Nope -> throw Exception("")
        is Yeah -> meat
    }
}
