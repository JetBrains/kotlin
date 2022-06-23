// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -MISSING_DEPENDENCY_SUPERCLASS

// FIXME: rename identifiers.kt

// FILE: 1.kt
package `check.pkg`

// FILE: 2.kt
package totally.normal.pkg

class `Check.Class`
class NormalClass {
    fun `check$member`() {}
}

object `Check;Object`
object NormalObject

data class Pair(val first: Int, val `next,one`: Int)

object Delegate {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any? = null
}

fun `check(function`() {
    val `check)variable` = 1
    val `check[delegated[variable` by Delegate

    val normalVariable = 2
    val normalDelegatedVariable by Delegate

    val (check, `destructuring]declaration`) = Pair(1, 2)
}

fun normalFunction() {}

val `check{property` = 1
val `check}delegated}property` by Delegate
val normalProperty = 2
val normalDelegatedProperty by Delegate

fun checkValueParameter(`check/parameter`: Int) {}

fun <`check<type<parameter`, normalTypeParameter> checkTypeParameter() {}

enum class `Check>Enum>Entry` {
    `CHECK:ENUM:ENTRY`;
}

typealias `check\typealias` = Any

fun `check&`() {}

fun `check~`() {}

fun `check*`() {}

fun `check?`() {}

fun `check#`() {}

fun `check|`() {}

fun `check§`() {}

fun `check%`() {}

fun `check@`() {}
