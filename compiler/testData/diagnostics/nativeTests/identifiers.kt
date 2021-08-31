// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -MISSING_DEPENDENCY_SUPERCLASS

// FIXME: rename identifiers.kt

// FILE: 1.kt
package <!INVALID_CHARACTERS_NATIVE_WARNING!>`check.pkg`<!>

// FILE: 2.kt
package totally.normal.pkg

class <!INVALID_CHARACTERS_NATIVE_WARNING!>`Check.Class`<!>
class NormalClass {
    fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check$member`<!>() {}
}

object <!INVALID_CHARACTERS_NATIVE_WARNING!>`Check;Object`<!>
object NormalObject

data class Pair(val first: Int, val <!INVALID_CHARACTERS_NATIVE_WARNING!>`next,one`<!>: Int)

object Delegate {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any? = null
}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check(function`<!>() {
    val <!INVALID_CHARACTERS_NATIVE_WARNING!>`check)variable`<!> = 1
    val <!INVALID_CHARACTERS_NATIVE_WARNING!>`check[delegated[variable`<!> by Delegate

    val normalVariable = 2
    val normalDelegatedVariable by Delegate

    val (check, <!INVALID_CHARACTERS_NATIVE_WARNING!>`destructuring]declaration`<!>) = Pair(1, 2)
}

fun normalFunction() {}

val <!INVALID_CHARACTERS_NATIVE_WARNING!>`check{property`<!> = 1
val <!INVALID_CHARACTERS_NATIVE_WARNING!>`check}delegated}property`<!> by Delegate
val normalProperty = 2
val normalDelegatedProperty by Delegate

fun checkValueParameter(<!INVALID_CHARACTERS_NATIVE_WARNING!>`check/parameter`<!>: Int) {}

fun <<!INVALID_CHARACTERS_NATIVE_WARNING!>`check<type<parameter`<!>, normalTypeParameter> checkTypeParameter() {}

enum class <!INVALID_CHARACTERS_NATIVE_WARNING!>`Check>Enum>Entry`<!> {
    <!INVALID_CHARACTERS_NATIVE_WARNING!>`CHECK:ENUM:ENTRY`<!>;
}

typealias <!INVALID_CHARACTERS_NATIVE_WARNING!>`check\typealias`<!> = Any

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check&`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check~`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check*`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check?`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check#`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check|`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check§`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check%`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_WARNING!>`check@`<!>() {}
