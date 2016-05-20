enum class EnumTarget { <caret>RED, GREEN, BLUE }

fun refer(p: EnumTarget) {
    println(p == EnumTarget.RED)
}