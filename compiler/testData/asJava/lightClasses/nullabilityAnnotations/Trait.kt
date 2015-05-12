// Trait

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

interface Trait {
    fun notNull(a: String): String
    fun nullable(a: String?): String?

    NotNull fun notNullWithNN(): String
    Nullable fun notNullWithN(): String

    Nullable fun nullableWithN(): String?
    NotNull fun nullableWithNN(): String?

    val nullableVal: String?
    var nullableVar: String?
    val notNullVal: String
    var notNullVar: String
}