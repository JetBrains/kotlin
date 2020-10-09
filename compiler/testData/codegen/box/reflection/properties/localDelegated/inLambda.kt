// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.*

inline operator fun String.getValue(t:Any?, p: KProperty<*>): String =
    if (p.returnType.classifier == String::class) this else "fail"

fun box() = {
    val x by "OK"
    x
}()
