// !CHECK_TYPE

data class A(val x: Int, val y: String)

fun foo(arr: Array<A>) {
    for ((b, c) in arr) {
        checkSubtype<Int>(b)
        checkSubtype<String>(c)
    }
}
