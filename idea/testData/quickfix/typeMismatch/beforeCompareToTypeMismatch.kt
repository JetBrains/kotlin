// "Change 'A.compareTo' function return type to 'Int'" "true"
trait A {
    fun compareTo(other: Any): String
}
fun foo(x: A) {
    if (x <<caret> 0) {}
}