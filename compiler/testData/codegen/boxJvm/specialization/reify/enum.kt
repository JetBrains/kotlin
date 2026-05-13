// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.enums.enumEntries

enum class Direction {
    UP, RIGHT, DOWN, LEFT
}

fun <@JvmSpecialize reified T: Enum<T>> values() = enumValues<T>()
fun <@JvmSpecialize reified T: Enum<T>> valueOf(name: String) = enumValueOf<T>(name)
fun <@JvmSpecialize reified T: Enum<T>> entries() = enumEntries<T>()

fun box(): String {
    if (!values<Direction>().contentEquals(enumValues<Direction>())) return "enumValues"
    if (valueOf<Direction>("UP") !== Direction.UP) return "valueOf"
    if (entries<Direction>() != listOf(Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT)) return "enumEntries"
    return "OK"
}
