import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

fun <R> property(property: KProperty0<R>): Int = 1
fun <T, R> property(property: KProperty1<T, R>): String = ""

val subject = ""

class O {
    val subject = ""
}

val someProperty0 = property(::subject)
val someProperty1 = property(O::subject)
