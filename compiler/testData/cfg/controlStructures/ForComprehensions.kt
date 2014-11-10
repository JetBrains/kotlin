fun IntRange.map<T>(f: (Int) -> T): List<T> = throw AssertionError("")

fun main() {
    foo(for (i in 1..3) yield i*i)

    val list = for (i in 1..3) yield {
        foo(i)
        i*i
    }

    val matrix = for (i in 1..3) yield for (j in 1..i) yield i*j
}

fun foo(n: Int) {

}

fun foo(list: List<Int>) {

}