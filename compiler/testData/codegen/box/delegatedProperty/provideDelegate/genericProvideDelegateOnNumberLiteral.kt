// WITH_STDLIB

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

operator fun <C, T> T.provideDelegate(thisRef: C, property: KProperty<*>) =
    object : ReadOnlyProperty<C, T> {
        override operator fun getValue(thisRef: C, property: KProperty<*>) = this@provideDelegate
    }

val byInt by 42
val byIntAsLong: Long by 42L
val byIntNullable: Int? by 42

val byString by "str"
val byStringNullable: String? = "strNullable"


fun box(): String {
    if (byInt != 42) return "fail1"
    if (byIntAsLong != 42L) return "fail2"
    if (byIntNullable != 42) return "fail3"

    if (byString != "str") return "fail4"
    if (byStringNullable != "strNullable") return "fail5"

    return "OK"
}
