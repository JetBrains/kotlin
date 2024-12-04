// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM

import java.util.*

inline fun <reified T : Enum<T>> emptyEnumSet(): EnumSet<T> = EnumSet.noneOf(T::class.java)
fun <T : Enum<T>> enumSetOf(e: T): EnumSet<T> = EnumSet.of(e)

enum class SomeEnum { ONE }
val set = enumSetOf(SomeEnum.ONE).takeIf { it.size > 0 } ?: emptyEnumSet()

fun box(): String {
    return if (set.first() == SomeEnum.ONE) "OK" else "NOK"
}
