// FIR_IDENTICAL
// WITH_REFLECT
import kotlin.reflect.KProperty0

data class MyPattern(
    val name: String,
    val conservation: String?,
    val awake: Double,
    val brainwt: Double?,
    val bodywt: Double,
)


internal inline fun <reified T> Iterable<T>.ggplot4(
    x: T.() -> KProperty0<*>,
    y: T.() -> KProperty0<*>,
) {
    // build df from data
    val map = map { x(it) to y(it) }
    map.first().first.name

    TODO("do something meaningful")
}

fun main() {
    listOf<MyPattern>().ggplot4(
        x = { ::conservation },
        y = { ::bodywt }
    )
}
