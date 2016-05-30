// "Make 'My' final" "true"

open class My(open val x: Int) {
    val y = <caret>x
}