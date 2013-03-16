// "Change 'A.compareTo' function return type to 'Int'" "true"
trait A {
    fun compareTo(other: Any): Int
}
fun foo(x: A) {
    if (x <<caret> 0) {}
}