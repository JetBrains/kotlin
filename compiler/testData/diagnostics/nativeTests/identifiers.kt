// IGNORE_NON_REVERSED_RESOLVE: KT-62937
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -MISSING_DEPENDENCY_SUPERCLASS

// FIXME: rename identifiers.kt

// FILE: 1.kt
package <!INVALID_CHARACTERS_NATIVE_ERROR!>`check.pkg`<!>

// FILE: 11.kt
package one.<!INVALID_CHARACTERS_NATIVE_ERROR!>`two.three`<!>.four.<!INVALID_CHARACTERS_NATIVE_ERROR!>`five.six`<!>.seven

// FILE: 2.kt
package totally.normal.pkg

class <!INVALID_CHARACTERS_NATIVE_ERROR!>`Check.Class`<!>
class NormalClass {
    fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check$member`<!>() {}
}

object <!INVALID_CHARACTERS_NATIVE_ERROR!>`Check;Object`<!>
object NormalObject

data class Pair(val first: Int, val <!INVALID_CHARACTERS_NATIVE_ERROR!>`next,one`<!>: Int)

object Delegate {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any? = null
}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check(function`<!>() {
    val <!INVALID_CHARACTERS_NATIVE_ERROR!>`check)variable`<!> = 1
    val <!INVALID_CHARACTERS_NATIVE_ERROR!>`check[delegated[variable`<!> by Delegate

    val normalVariable = 2
    val normalDelegatedVariable by Delegate

    val (check, <!INVALID_CHARACTERS_NATIVE_ERROR!>`destructuring]declaration`<!>) = Pair(1, 2)
}

fun normalFunction() {}

val <!INVALID_CHARACTERS_NATIVE_ERROR!>`check{property`<!> = 1
val <!INVALID_CHARACTERS_NATIVE_ERROR!>`check}delegated}property`<!> by Delegate
val normalProperty = 2
val normalDelegatedProperty by Delegate

fun checkValueParameter(<!INVALID_CHARACTERS_NATIVE_ERROR!>`check/parameter`<!>: Int) {}

fun <<!INVALID_CHARACTERS_NATIVE_ERROR!>`check<type<parameter`<!>, normalTypeParameter> checkTypeParameter() {}

enum class <!INVALID_CHARACTERS_NATIVE_ERROR!>`Check>Enum>Entry`<!> {
    <!INVALID_CHARACTERS_NATIVE_ERROR!>`CHECK:ENUM:ENTRY`<!>;
}

typealias <!INVALID_CHARACTERS_NATIVE_ERROR!>`check\typealias`<!> = Any

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check&`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check~`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check*`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check?`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check#`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check|`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check§`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check%`<!>() {}

fun <!INVALID_CHARACTERS_NATIVE_ERROR!>`check@`<!>() {}
