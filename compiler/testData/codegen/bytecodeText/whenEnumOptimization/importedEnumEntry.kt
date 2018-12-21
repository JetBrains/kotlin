// IGNORE_BACKEND: JVM_IR
import Color.RED

enum class Color { RED, GREEN, BLUE }

fun fifth(arg: Color) = when (arg) {
    RED -> 1
    Color.GREEN -> 2
    Color.BLUE -> 3
}


// 1 TABLESWITCH
// 0 LOOKUPSWITCH