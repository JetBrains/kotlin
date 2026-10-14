// FILE: main.kt
package a

import a.original as renamed
import a.Color.RED as Dark

val original = 1
enum class Color {
    RED
}

const val FUN_ALIAS = ::renamed.name
const val ENUM_ALIAS = Dark.name

fun <T> T.id() = this

fun error(propertyName: String, constant: String, runtime: String): String {
    return "There is a difference for $propertyName: const=$constant runtime=$runtime"
}

fun box(): String {
    if (FUN_ALIAS != (::renamed).id().name) {
        return error("FUN_ALIAS", FUN_ALIAS, (::renamed).id().name)
    }

    if (ENUM_ALIAS != Dark.id().name) {
        return error("ENUM_ALIAS", ENUM_ALIAS, Dark.id().name)
    }

    return "OK"
}
