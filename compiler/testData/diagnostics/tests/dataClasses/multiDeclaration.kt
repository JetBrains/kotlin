data class A(val x: Int, val y: String)

fun foo(a: A) {
    val (b, c) = a
    b : Int
    c : String
}
