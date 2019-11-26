class A {
    fun unsafeCall()
}

fun getNull():A?{
    return null
}

fun unsafeFoo(): A? {
    return getNull()<caret>!!
}