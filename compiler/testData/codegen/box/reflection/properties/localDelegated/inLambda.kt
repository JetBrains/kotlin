// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.*

fun <T> eval(fn: () -> T) = fn()

inline operator fun String.getValue(t:Any?, p: KProperty<*>): String =
    if (p.returnType.classifier == String::class) this else "fail"

fun box() = eval {
    val x by "OK"
    x
}
