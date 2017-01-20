object A {
    val a = 42
    val b = A.a
}

fun main(args : Array<String>) {
    println(A.b)
}