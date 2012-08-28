data class A(val x: Int, val y: String)

fun foo(arr: Array<A>) {
    for ((b, c) in arr) {
        b : Int
        c : String
    }
}
