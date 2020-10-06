fun Y.test(): String? {
    val a = when (this) {
        is F -> 1
        is G -> 2
        else -> return null
    }
    return null
}
