package test

inline fun <T> doSmth(a: T) : Boolean {
    return a.identityEquals(a)
}