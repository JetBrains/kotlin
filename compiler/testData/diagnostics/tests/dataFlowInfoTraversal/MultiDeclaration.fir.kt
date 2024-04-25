// CHECK_TYPE

operator fun Int.component1() = "a"

fun foo(a: Number) {
    val (x) = a as Int
    checkSubtype<Int>(a)
    checkSubtype<String>(x)
}
