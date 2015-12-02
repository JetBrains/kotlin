fun foo() {
    val a = 1
    val <caret>x = " $a"
    val y = "x=${x}_"
}