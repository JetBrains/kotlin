fun IntRange.flatMap<T>(f: (Int) -> List<T>): List<T> = throw AssertionError("")
fun IntRange.map<T>(f: (Int) -> T): List<T> = throw AssertionError("")

fun main() {
    foo(for (i in 1..3, j in 1..i) yield i*j)
}

fun foo(list: List<Int>) {

}