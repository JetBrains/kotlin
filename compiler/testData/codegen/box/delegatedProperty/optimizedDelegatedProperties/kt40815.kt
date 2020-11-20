import kotlin.reflect.KProperty

operator fun Int.provideDelegate(thiz: Any?, property: KProperty<*>): String = property.name
inline operator fun String.getValue(thiz: Any?, property: KProperty<*>): String = property.name

fun box(): String = with(42) {
    val O by this
    O
} + {
    val K by ""
    K
}()
