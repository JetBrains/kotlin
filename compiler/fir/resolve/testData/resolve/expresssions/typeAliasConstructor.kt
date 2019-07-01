class A(i: Int)

typealias AA = A

class B<T>(t: T)

typealias BB<U> = B<U>

fun main() {
    val x = AA(1)
    val y = BB<String>("bb")
}

