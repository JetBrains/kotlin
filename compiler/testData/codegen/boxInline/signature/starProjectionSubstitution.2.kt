package test

interface F<T> {
    fun test(p: T) : Int
}

inline fun <T> Array<T>.copyOfRange1(crossinline toIndex: () -> Int) =
        object : F<T> {
            override fun test(p: T): Int {
                return toIndex()
            }
        }
