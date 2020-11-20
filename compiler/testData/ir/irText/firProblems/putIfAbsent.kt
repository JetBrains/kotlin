// WITH_RUNTIME
// FULL_JDK

class Owner {
    fun <T> foo(x: T, y: T) {
        val map = mutableMapOf<T, T>()
        map.putIfAbsent(x, y)
    }
}