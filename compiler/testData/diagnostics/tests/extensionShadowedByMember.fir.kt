// ISSUE: KT-54483
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

abstract class Cache {
    abstract fun <T> getTyped(key: Any, klass: Class<T>): T

    fun get(key: Any): Int = 10
}

inline fun <reified T> Cache.get(key: Any) = getTyped(key, T::class.java)

fun Cache.getString(key: Any) = get<String>(key)
